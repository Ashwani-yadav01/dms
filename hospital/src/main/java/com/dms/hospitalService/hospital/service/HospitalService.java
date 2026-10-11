package com.dms.hospitalService.hospital.service;

import com.dms.hospitalService.hospital.dto.request.HospitalCreateRequest;
import com.dms.hospitalService.hospital.dto.request.HospitalUpdateRequest;
import com.dms.hospitalService.hospital.dto.response.HospitalResponse;
import com.dms.hospitalService.hospital.entity.HospitalStatus;
import com.dms.hospitalService.hospital.entity.MedicalSpeciality;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface HospitalService {
    HospitalResponse createHospital(HospitalCreateRequest request);
    HospitalResponse getHospitalById(UUID id);
    HospitalResponse toggleAcceptingPatients(UUID id, Boolean status);

    // The Smart Routing Method: Finds nearest hospitals with specific capabilities and available beds
    List<HospitalResponse> findNearestCapableHospitals(
            Double latitude,
            Double longitude,
            Double radiusKm,
            Set<MedicalSpeciality> requiredSpecialities,
            Boolean requiresIcu
    );
    void deleteHospital(UUID id);

    // Admin operations
    Page<HospitalResponse> listHospitals(Pageable pageable, HospitalStatus status);
    HospitalResponse createHospitalAdmin(HospitalCreateRequest request);
    HospitalResponse updateHospital(UUID id, HospitalUpdateRequest request);
    HospitalResponse changeHospitalStatus(UUID id, HospitalStatus newStatus);
}