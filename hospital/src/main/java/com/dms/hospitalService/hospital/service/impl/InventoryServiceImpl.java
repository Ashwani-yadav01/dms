package com.dms.hospitalService.hospital.service.impl;

import com.dms.hospitalService.hospital.dto.request.InventoryCreateRequest;
import com.dms.hospitalService.hospital.dto.request.InventoryUpdateRequest;
import com.dms.hospitalService.hospital.dto.response.InventoryResponse;
import com.dms.hospitalService.hospital.entity.Hospital;
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

        if (inventory.getCurrentQuantity() <= inventory.getCriticalThreshold()) {
            InventoryShortageAlertEvent alert = InventoryShortageAlertEvent.builder()
                    .hospitalId(hospitalId)
                    .itemType(inventory.getItemType())
                    .currentQuantity(inventory.getCurrentQuantity())
                    .criticalThreshold(inventory.getCriticalThreshold())
                    .alertTime(LocalDateTime.now())
                    .build();
            kafkaProducer.sendInventoryShortageAlert(alert);
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