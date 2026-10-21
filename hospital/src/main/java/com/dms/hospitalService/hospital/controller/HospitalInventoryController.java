package com.dms.hospitalService.hospital.controller;

import com.dms.hospitalService.hospital.dto.response.HospitalResponse;
import com.dms.hospitalService.hospital.entity.DistrictMedicalReserve;
import com.dms.hospitalService.hospital.entity.HospitalDeliveryReceipt;
import com.dms.hospitalService.hospital.entity.HospitalSupplyRequest;
import com.dms.hospitalService.hospital.entity.InventoryItemType;
import com.dms.hospitalService.hospital.entity.MedicalInventory;
import com.dms.hospitalService.hospital.entity.SupplyRequestStatus;
import com.dms.hospitalService.hospital.repository.DistrictMedicalReserveRepository;
import com.dms.hospitalService.hospital.repository.HospitalDeliveryReceiptRepository;
import com.dms.hospitalService.hospital.repository.HospitalSupplyRequestRepository;
import com.dms.hospitalService.hospital.repository.MedicalInventoryRepository;
import com.dms.hospitalService.hospital.service.HospitalInventoryAlertService;
import com.dms.hospitalService.hospital.service.HospitalService;
import com.dms.hospitalService.hospital.service.InventoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/hospitals")
@Slf4j
public class HospitalInventoryController {

    private final HospitalInventoryAlertService alertService;
    private final HospitalService hospitalService;
    private final HospitalDeliveryReceiptRepository deliveryReceiptRepository;
    private final HospitalSupplyRequestRepository supplyRequestRepository;
    private final InventoryService inventoryService;
    private final MedicalInventoryRepository medicalInventoryRepository;
    private final DistrictMedicalReserveRepository districtReserveRepository;

    public HospitalInventoryController(
            HospitalInventoryAlertService alertService,
            HospitalService hospitalService,
            HospitalDeliveryReceiptRepository deliveryReceiptRepository,
            HospitalSupplyRequestRepository supplyRequestRepository,
            InventoryService inventoryService,
            MedicalInventoryRepository medicalInventoryRepository,
            DistrictMedicalReserveRepository districtReserveRepository) {
        this.alertService = alertService;
        this.hospitalService = hospitalService;
        this.deliveryReceiptRepository = deliveryReceiptRepository;
        this.supplyRequestRepository = supplyRequestRepository;
        this.inventoryService = inventoryService;
        this.medicalInventoryRepository = medicalInventoryRepository;
        this.districtReserveRepository = districtReserveRepository;
    }

    // Supports both /inventory/shortage and /supplies/request
    @PostMapping(value = {"/{hospitalId}/inventory/shortage", "/{hospitalId}/supplies/request"})
    public ResponseEntity<?> declareShortage(
            HttpServletRequest httpRequest,
            @PathVariable UUID hospitalId,
            @RequestBody ShortageRequest request) {

        if (!canManageHospital(httpRequest, hospitalId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden: You are not authorized to declare shortages for this hospital.");
        }

        String hospitalName = "Hospital Facility";
        try {
            HospitalResponse h = hospitalService.getHospitalById(hospitalId);
            if (h != null && h.getName() != null) {
                hospitalName = h.getName();
            }
        } catch (Exception ignored) {}

        String urgency = request.urgencyLevel() != null ? request.urgencyLevel().toUpperCase() : "CRITICAL";

        HospitalSupplyRequest supplyRequest = HospitalSupplyRequest.builder()
                .hospitalId(hospitalId)
                .hospitalName(hospitalName)
                .itemType(request.itemType())
                .requestedQuantity(request.requestedQuantity())
                .urgencyLevel(urgency)
                .status(SupplyRequestStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        HospitalSupplyRequest saved = supplyRequestRepository.save(supplyRequest);

        // Also trigger background alert broadcast for event stream
        try {
            alertService.triggerShortageAlert(hospitalId, request.itemType(), request.requestedQuantity(), urgency);
        } catch (Exception e) {
            log.warn("Non-fatal: Shortage alert event broadcast skipped: {}", e.getMessage());
        }

        return ResponseEntity.accepted().body(saved);
    }

    // --- JOB 2: SUPPLY DEMANDS PRIORITIZED QUEUE (DISTRICT ADMIN) ---

    @GetMapping("/supplies/requests")
    public ResponseEntity<List<HospitalSupplyRequest>> getAllSupplyRequests(
            HttpServletRequest httpRequest,
            @RequestParam(required = false) SupplyRequestStatus status) {

        if (!isAuthorizedHospitalAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<HospitalSupplyRequest> list;
        if (status != null) {
            list = supplyRequestRepository.findByStatusPrioritized(status);
        } else {
            list = supplyRequestRepository.findAllPrioritized();
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{hospitalId}/supplies/requests")
    public ResponseEntity<List<HospitalSupplyRequest>> getHospitalSupplyRequests(
            HttpServletRequest httpRequest,
            @PathVariable UUID hospitalId) {

        if (!canManageHospital(httpRequest, hospitalId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(supplyRequestRepository.findByHospitalIdOrderByCreatedAtDesc(hospitalId));
    }

    @PatchMapping("/supplies/requests/{requestId}/approve")
    public ResponseEntity<?> approveSupplyRequest(
            HttpServletRequest httpRequest,
            @PathVariable UUID requestId) {

        if (!isAuthorizedHospitalAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden: Only District Admin (Medical Management) or Super Admin can approve supply requests.");
        }

        HospitalSupplyRequest req = supplyRequestRepository.findById(requestId)
                .orElse(null);
        if (req == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Supply request not found.");
        }

        if (req.getStatus() == SupplyRequestStatus.APPROVED) {
            return ResponseEntity.ok(req);
        }

        UUID adminId = extractUserId(httpRequest);

        // Deduct from District Central Reserves
        InventoryItemType resolvedType = resolveItemType(req.getItemType());
        if (resolvedType != null) {
            DistrictMedicalReserve reserve = getOrCreateDistrictReserve(resolvedType);
            if (reserve.getCurrentQuantity() < req.getRequestedQuantity()) {
                log.warn("Cannot approve request {}: Insufficient district stock. Requested {}, available {}",
                        requestId, req.getRequestedQuantity(), reserve.getCurrentQuantity());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Insufficient stock in District Reserves Depot to fulfill this request. Available: " 
                              + reserve.getCurrentQuantity() + ", Requested: " + req.getRequestedQuantity());
            }
            int newReserveQty = reserve.getCurrentQuantity() - req.getRequestedQuantity();
            reserve.setCurrentQuantity(newReserveQty);
            districtReserveRepository.save(reserve);
            log.info("📉 Deducted {} units of {} from District Reserves Depot. Remaining district stockpile: {}",
                    req.getRequestedQuantity(), resolvedType, newReserveQty);
        }

        req.setStatus(SupplyRequestStatus.APPROVED);
        req.setActionedAt(Instant.now());
        req.setActionedByAdminId(adminId);

        // Auto-increment hospital local inventory
        try {
            inventoryService.restockItemFromDispatch(
                    req.getHospitalId(),
                    req.getItemType(),
                    req.getRequestedQuantity()
            );
        } catch (Exception e) {
            log.error("Failed to restock item for hospital {}: {}", req.getHospitalId(), e.getMessage(), e);
        }

        // Record receipt in delivery receipt audit
        try {
            HospitalDeliveryReceipt receipt = HospitalDeliveryReceipt.builder()
                    .dispatchOrderId(UUID.randomUUID())
                    .hospitalId(req.getHospitalId())
                    .warehouseName("District Medical Relief Directorate")
                    .itemType(req.getItemType())
                    .deliveredQuantity(req.getRequestedQuantity())
                    .vehicleNumber("DDMA-DIRECT-FULFILLMENT")
                    .receivedAt(Instant.now())
                    .build();
            deliveryReceiptRepository.save(receipt);
        } catch (Exception e) {
            log.warn("Could not save delivery receipt audit: {}", e.getMessage());
        }

        HospitalSupplyRequest updated = supplyRequestRepository.save(req);
        log.info("✅ Supply request {} approved by Admin {}. Restocked {} units of {} for hospital {}",
                requestId, adminId, req.getRequestedQuantity(), req.getItemType(), req.getHospitalId());

        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/supplies/requests/{requestId}/reject")
    public ResponseEntity<?> rejectSupplyRequest(
            HttpServletRequest httpRequest,
            @PathVariable UUID requestId,
            @RequestParam(required = false, defaultValue = "Demand rejected by District Medical Management") String reason) {

        if (!isAuthorizedHospitalAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden: Only District Admin (Medical Management) or Super Admin can reject supply requests.");
        }

        HospitalSupplyRequest req = supplyRequestRepository.findById(requestId)
                .orElse(null);
        if (req == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Supply request not found.");
        }

        UUID adminId = extractUserId(httpRequest);
        req.setStatus(SupplyRequestStatus.REJECTED);
        req.setAdminNotes(reason);
        req.setActionedAt(Instant.now());
        req.setActionedByAdminId(adminId);

        HospitalSupplyRequest updated = supplyRequestRepository.save(req);
        log.info("❌ Supply request {} rejected by Admin {}. Reason: {}", requestId, adminId, reason);

        return ResponseEntity.ok(updated);
    }

    // --- JOB 3: DISTRICT-WIDE MEDICAL RESERVES & CAPACITY OVERVIEW ---

    @GetMapping("/inventory/district-overview")
    public ResponseEntity<?> getDistrictOverview(HttpServletRequest httpRequest) {
        if (!isAuthorizedHospitalAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Return District Central Depot Stockpile from DistrictMedicalReserve
        Map<String, Integer> stockTotals = new HashMap<>();
        for (InventoryItemType type : InventoryItemType.values()) {
            DistrictMedicalReserve reserve = getOrCreateDistrictReserve(type);
            stockTotals.put(type.name(), reserve.getCurrentQuantity());
        }

        var hospitalsPage = hospitalService.listHospitals(Pageable.ofSize(100), null);
        List<HospitalResponse> hospitalList = hospitalsPage != null ? hospitalsPage.getContent() : Collections.emptyList();

        int availableGeneralBeds = hospitalList.stream().mapToInt(h -> h.getAvailableGeneralBeds() != null ? h.getAvailableGeneralBeds() : 0).sum();
        int availableIcuBeds = hospitalList.stream().mapToInt(h -> h.getAvailableIcuBeds() != null ? h.getAvailableIcuBeds() : 0).sum();

        Map<String, Object> response = new HashMap<>();
        response.put("stockTotals", stockTotals);
        response.put("totalHospitals", hospitalList.size());
        response.put("availableGeneralBeds", availableGeneralBeds);
        response.put("availableIcuBeds", availableIcuBeds);
        response.put("hospitals", hospitalList);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/inventory/district-reserves/adjust")
    public ResponseEntity<?> adjustDistrictReserve(
            HttpServletRequest httpRequest,
            @RequestBody DistrictReserveAdjustRequest request) {

        if (!isAuthorizedHospitalAdmin(httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden: Only District Admin or Super Admin can adjust district reserves.");
        }

        InventoryItemType type = resolveItemType(request.itemType());
        if (type == null) {
            return ResponseEntity.badRequest().body("Invalid item type: " + request.itemType());
        }

        DistrictMedicalReserve reserve = getOrCreateDistrictReserve(type);
        int newQty = reserve.getCurrentQuantity() + request.quantityChange();
        if (newQty < 0) {
            return ResponseEntity.badRequest().body("Cannot deduct more than available district stock.");
        }
        reserve.setCurrentQuantity(newQty);
        DistrictMedicalReserve updated = districtReserveRepository.save(reserve);
        log.info("District reserve for {} adjusted by {}. New depot total: {}",
                type, request.quantityChange(), newQty);

        return ResponseEntity.ok(updated);
    }

    private DistrictMedicalReserve getOrCreateDistrictReserve(InventoryItemType itemType) {
        return districtReserveRepository.findByItemType(itemType).orElseGet(() -> {
            int defaultBaseline = getDefaultBaseline(itemType);
            DistrictMedicalReserve newReserve = DistrictMedicalReserve.builder()
                    .itemType(itemType)
                    .currentQuantity(defaultBaseline)
                    .build();
            return districtReserveRepository.save(newReserve);
        });
    }

    private int getDefaultBaseline(InventoryItemType itemType) {
        if (itemType == null) return 100;
        switch (itemType) {
            case O_NEGATIVE_BLOOD: return 500;
            case OXYGEN_CYLINDER: return 300;
            case BURN_KIT: return 200;
            case ANTIBIOTICS: return 500;
            case CHOLERA_VACCINE: return 300;
            case ANTI_VENOM: return 150;
            case DECON_SOLUTION: return 200;
            case TRAUMA_KIT: return 400;
            case VENTILATOR: return 100;
            default: return 100;
        }
    }

    private InventoryItemType resolveItemType(String typeStr) {
        if (typeStr == null) return null;
        try {
            return InventoryItemType.valueOf(typeStr.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }


    @GetMapping("/{hospitalId}/delivery-receipts")
    public ResponseEntity<List<HospitalDeliveryReceipt>> getDeliveryReceipts(
            HttpServletRequest httpRequest,
            @PathVariable UUID hospitalId) {

        if (!canManageHospital(httpRequest, hospitalId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(deliveryReceiptRepository.findByHospitalIdOrderByReceivedAtDesc(hospitalId));
    }

    private boolean isAuthorizedHospitalAdmin(HttpServletRequest request) {
        String role = request.getHeader("X-User-Role");
        if ("SUPER_ADMIN".equals(role)) {
            return true;
        }
        if ("DISTRICT_ADMIN".equals(role)) {
            String deptCategory = request.getHeader("X-User-Department-Category");
            return deptCategory == null || deptCategory.isBlank()
                    || "MEDICAL_RESPONSE".equals(deptCategory)
                    || "MEDICAL_MANAGEMENT".equals(deptCategory)
                    || "HOSPITAL_MANAGEMENT".equals(deptCategory)
                    || "DEPARTMENT_MANAGEMENT".equals(deptCategory);
        }
        if ("GOVERNMENT_OFFICIAL".equals(role)) {
            String deptCategory = request.getHeader("X-User-Department-Category");
            return "MEDICAL_RESPONSE".equals(deptCategory) || "MEDICAL_MANAGEMENT".equals(deptCategory) || "HOSPITAL_MANAGEMENT".equals(deptCategory);
        }
        return false;
    }

    private boolean canManageHospital(HttpServletRequest request, UUID hospitalId) {
        if (isAuthorizedHospitalAdmin(request)) {
            return true;
        }
        String role = request.getHeader("X-User-Role");
        if ("HOSPITAL".equals(role)) {
            UUID actorId = extractUserId(request);
            if (actorId == null) return false;
            try {
                HospitalResponse h = hospitalService.getHospitalById(hospitalId);
                return h.getAdminUserId() != null && h.getAdminUserId().equals(actorId);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private UUID extractUserId(HttpServletRequest request) {
        String idHeader = request.getHeader("X-User-Id");
        if (idHeader != null && !idHeader.isBlank()) {
            try {
                return UUID.fromString(idHeader);
            } catch (Exception ignored) {}
        }
        return null;
    }

    public record ShortageRequest(String itemType, int requestedQuantity, String urgencyLevel) {}
    public record DistrictReserveAdjustRequest(String itemType, int quantityChange) {}
}