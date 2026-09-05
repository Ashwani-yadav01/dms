package com.dms.hospitalService.hospital.service.impl;

import com.dms.hospitalService.hospital.dto.request.InventoryCreateRequest;
import com.dms.hospitalService.hospital.dto.request.InventoryUpdateRequest;
import com.dms.hospitalService.hospital.dto.response.InventoryResponse;
import com.dms.hospitalService.hospital.entity.Hospital;
import com.dms.hospitalService.hospital.entity.InventoryItemType;
import com.dms.hospitalService.hospital.entity.MedicalInventory;
import com.dms.hospitalService.hospital.kafka.event.InventoryShortageAlertEvent;
import com.dms.hospitalService.hospital.kafka.producer.HospitalKafkaProducer;
import com.dms.hospitalService.hospital.repository.HospitalRepository;
import com.dms.hospitalService.hospital.repository.MedicalInventoryRepository;
import com.dms.hospitalService.hospital.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final MedicalInventoryRepository inventoryRepository;
    private final HospitalRepository hospitalRepository;
    private final HospitalKafkaProducer kafkaProducer;

    @Override
    @Transactional
    public InventoryResponse initializeInventory(UUID hospitalId, InventoryCreateRequest request) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("Hospital not found"));

        // Check if item already exists
        inventoryRepository.findByHospitalIdAndItemTypeForUpdate(hospitalId, request.getItemType())
                .ifPresent(i -> { throw new IllegalStateException("Inventory item already exists. Use update instead."); });

        MedicalInventory inventory = MedicalInventory.builder()
                .hospital(hospital)
                .itemType(request.getItemType())
                .currentQuantity(request.getInitialQuantity())
                .criticalThreshold(request.getCriticalThreshold())
                .build();

        return mapToResponse(inventoryRepository.save(inventory));
    }

    @Override
    @Transactional
    public InventoryResponse updateInventoryQuantity(UUID hospitalId, InventoryUpdateRequest request) {
        // 🚨 PESSIMISTIC WRITE LOCK for thread-safe stock updates
        MedicalInventory inventory = inventoryRepository.findByHospitalIdAndItemTypeForUpdate(hospitalId, request.getItemType())
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found"));

        int newQuantity = inventory.getCurrentQuantity() + request.getQuantityChange();
        if (newQuantity < 0) {
            throw new IllegalStateException("Insufficient inventory. Cannot consume more than available stock.");
        }

        inventory.setCurrentQuantity(newQuantity);

        if (request.getCriticalThreshold() != null) {
            inventory.setCriticalThreshold(request.getCriticalThreshold());
        }

        inventory = inventoryRepository.save(inventory);

        // 🚀 UPDATED KAFKA TRIGGER FOR LOGISTICS SERVICE
        if (inventory.getCurrentQuantity() <= inventory.getCriticalThreshold()) {
            Hospital hospital = inventory.getHospital(); // Get hospital for GPS coordinates

            // Calculate how much Logistics should send (e.g., restock to 3x the critical threshold)
            int requestedRestockQty = Math.max(50, (inventory.getCriticalThreshold() * 3) - inventory.getCurrentQuantity());

            String urgency = inventory.getCurrentQuantity() == 0 ? "CRITICAL" : "HIGH";

            InventoryShortageAlertEvent alert = InventoryShortageAlertEvent.builder()
                    .hospitalId(hospital.getId())
                    .hospitalName(hospital.getName())             // Required for Logistics UI/Logs
                    .hospitalLatitude(hospital.getLatitude())     // Required for Haversine Routing
                    .hospitalLongitude(hospital.getLongitude())   // Required for Haversine Routing
                    .itemType(inventory.getItemType().name())
                    .currentStock(inventory.getCurrentQuantity())
                    .requestedQuantity(requestedRestockQty)       // Tells Logistics how many units to dispatch
                    .urgencyLevel(urgency)
                    .incomingCasualtyCount(0)
                    .timestamp(Instant.now())
                    .build();

            kafkaProducer.sendInventoryShortageAlert(alert);
            log.info("🚨 Published automated restocking alert to Logistics for {} units of {}",
                    requestedRestockQty, inventory.getItemType());
        }

        return mapToResponse(inventory);
    }
    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getHospitalInventory(UUID hospitalId) {
        return inventoryRepository.findByHospitalId(hospitalId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    @Override
    @Transactional
    public void restockItemFromDispatch(UUID hospitalId, String itemTypeName, int quantity) {
        log.info("📦 Restocking inventory for hospital: {}, item: {}, qty: +{}",
                hospitalId, itemTypeName, quantity);

        InventoryItemType itemTypeEnum;
        try {
            itemTypeEnum = InventoryItemType.valueOf(itemTypeName.toUpperCase().trim());
        } catch (IllegalArgumentException ex) {
            log.error("Unknown ItemType: {}. Cannot restock inventory.", itemTypeName);
            return;
        }

        // Use pessimistic lock to prevent race conditions during updates
        inventoryRepository.findByHospitalIdAndItemTypeForUpdate(hospitalId, itemTypeEnum)
                .ifPresentOrElse(inventory -> {
                    int updatedQuantity = inventory.getCurrentQuantity() + quantity;
                    inventory.setCurrentQuantity(updatedQuantity);
                    inventoryRepository.save(inventory);
                    log.info("✅ Restocked successfully. Hospital: {}, Item: {}, New Total: {}",
                            hospitalId, itemTypeName, updatedQuantity);
                }, () -> {
                    log.warn("Inventory entry not found for Hospital: {} and Item: {}. Creating item initial baseline.",
                            hospitalId, itemTypeName);
                    Hospital hospital = hospitalRepository.findById(hospitalId)
                            .orElseThrow(() -> new IllegalArgumentException("Hospital not found: " + hospitalId));

                    MedicalInventory newInventory = MedicalInventory.builder()
                            .hospital(hospital)
                            .itemType(itemTypeEnum)
                            .currentQuantity(quantity)
                            .criticalThreshold(20) // Safe default threshold
                            .build();
                    inventoryRepository.save(newInventory);
                });
    }
    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getCriticalShortages() {
        return inventoryRepository.findAllCriticalShortages().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    @Override
    @Transactional
    public void deleteInventoryItem(UUID id) {
        if (!inventoryRepository.existsById(id)) {
            throw new IllegalArgumentException("Inventory item not found");
        }
        inventoryRepository.deleteById(id);
        log.info("Deleted inventory item ID: {}", id);
    }
    private InventoryResponse mapToResponse(MedicalInventory i) {
        return InventoryResponse.builder()
                .id(i.getId())
                .hospitalId(i.getHospital().getId())
                .itemType(i.getItemType())
                .currentQuantity(i.getCurrentQuantity())
                .criticalThreshold(i.getCriticalThreshold())
                .isCriticalShortage(i.getCurrentQuantity() <= i.getCriticalThreshold())
                .lastUpdated(i.getLastUpdated())
                .build();
    }
}