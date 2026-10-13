package com.dms.hospitalService.hospital.adapter;

import com.dms.hospitalService.hospital.dto.response.InventoryResponse;
import java.util.List;
import java.util.UUID;

/**
 * Extension point for external healthcare ERP, HL7, or FHIR R4 supply integration.
 * Enables plugging external healthcare feeds without breaking internal DMS supply workflows.
 */
public interface ExternalSupplySyncAdapter {
    String getSourceSystemName();
    List<InventoryResponse> syncSuppliesFromSource(UUID hospitalId);
}
