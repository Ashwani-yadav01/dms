package com.dms.hospitalService.hospital.service;

import com.dms.hospitalService.hospital.dto.request.PatientAdmissionRequest;
import com.dms.hospitalService.hospital.dto.response.PatientAdmissionResponse;
import com.dms.hospitalService.hospital.entity.AdmissionStatus;
import com.dms.hospitalService.hospital.entity.Hospital;
import com.dms.hospitalService.hospital.entity.PatientAdmission;
import com.dms.hospitalService.hospital.entity.TriageLevel;
import com.dms.hospitalService.hospital.repository.HospitalRepository;
import com.dms.hospitalService.hospital.repository.PatientAdmissionRepository;
import com.dms.hospitalService.hospital.service.impl.PatientAdmissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientAdmissionServiceTest {

    @Mock
    private PatientAdmissionRepository admissionRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @InjectMocks
    private PatientAdmissionServiceImpl admissionService;

    private Hospital hospital;
    private UUID hospitalId;
    private UUID incidentId;

    @BeforeEach
    void setUp() {
        hospitalId = UUID.randomUUID();
        incidentId = UUID.randomUUID();

        hospital = Hospital.builder()
                .id(hospitalId)
                .name("City General Hospital")
                .isAcceptingPatients(true)
                .totalGeneralBeds(50)
                .availableGeneralBeds(10)
                .totalIcuBeds(10)
                .availableIcuBeds(2)
                .build();
    }

    @Test
    void testAdmitPatient_Success_GeneralBed() {
        PatientAdmissionRequest request = new PatientAdmissionRequest();
        request.setIncidentId(incidentId);
        request.setTriageLevel(TriageLevel.YELLOW);
        request.setRequiresIcu(false);
        request.setPatientName("Aarav Sharma");
        request.setAge(34);
        request.setGender("MALE");
        request.setTriageNotes("Fractured left arm");

        when(hospitalRepository.findByIdForUpdate(hospitalId)).thenReturn(Optional.of(hospital));
        when(admissionRepository.save(any(PatientAdmission.class))).thenAnswer(invocation -> {
            PatientAdmission pa = invocation.getArgument(0);
            pa.setId(UUID.randomUUID());
            return pa;
        });

        PatientAdmissionResponse response = admissionService.admitPatient(hospitalId, request);

        assertNotNull(response);
        assertEquals("Aarav Sharma", response.getPatientName());
        assertEquals(34, response.getAge());
        assertEquals(AdmissionStatus.ADMITTED, response.getStatus());
        assertEquals(9, hospital.getAvailableGeneralBeds()); // Decremented
        verify(hospitalRepository, times(1)).save(hospital);
    }

    @Test
    void testAdmitPatient_Success_IcuBed() {
        PatientAdmissionRequest request = new PatientAdmissionRequest();
        request.setIncidentId(incidentId);
        request.setTriageLevel(TriageLevel.RED);
        request.setRequiresIcu(true);
        request.setPatientName("Priya Verma");

        when(hospitalRepository.findByIdForUpdate(hospitalId)).thenReturn(Optional.of(hospital));
        when(admissionRepository.save(any(PatientAdmission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientAdmissionResponse response = admissionService.admitPatient(hospitalId, request);

        assertNotNull(response);
        assertEquals(1, hospital.getAvailableIcuBeds()); // Decremented from 2 to 1
    }

    @Test
    void testAdmitPatient_Success_WithContactAndNationalIdAndIncidentCause() {
        PatientAdmissionRequest request = new PatientAdmissionRequest();
        request.setTriageLevel(TriageLevel.RED);
        request.setRequiresIcu(false);
        request.setPatientName("Sunita Devi");
        request.setAge(42);
        request.setGender("FEMALE");
        request.setContactNumber("+91-9876543210");
        request.setNationalId("1234-5678-9012");
        request.setIncidentCause("Factory Chemical Boiler Flash Explosion");
        request.setTriageNotes("Second degree chemical burns across forearm");

        when(hospitalRepository.findByIdForUpdate(hospitalId)).thenReturn(Optional.of(hospital));
        when(admissionRepository.save(any(PatientAdmission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientAdmissionResponse response = admissionService.admitPatient(hospitalId, request);

        assertNotNull(response);
        assertEquals("Sunita Devi", response.getPatientName());
        assertEquals("+91-9876543210", response.getContactNumber());
        assertEquals("1234-5678-9012", response.getNationalId());
        assertEquals("Factory Chemical Boiler Flash Explosion", response.getIncidentCause());
        assertNull(response.getIncidentId()); // Verified optional incidentId!
        assertEquals(9, hospital.getAvailableGeneralBeds());
    }

    @Test
    void testAdmitPatient_ThrowsWhenNotAccepting() {
        hospital.setIsAcceptingPatients(false);
        PatientAdmissionRequest request = new PatientAdmissionRequest();
        request.setIncidentId(incidentId);
        request.setRequiresIcu(false);

        when(hospitalRepository.findByIdForUpdate(hospitalId)).thenReturn(Optional.of(hospital));

        assertThrows(IllegalStateException.class, () -> admissionService.admitPatient(hospitalId, request));
    }

    @Test
    void testDischargePatient_ReleasesBed() {
        UUID admissionId = UUID.randomUUID();
        PatientAdmission admission = PatientAdmission.builder()
                .id(admissionId)
                .hospital(hospital)
                .incidentId(incidentId)
                .requiresIcu(false)
                .status(AdmissionStatus.ADMITTED)
                .build();

        when(admissionRepository.findById(admissionId)).thenReturn(Optional.of(admission));
        when(hospitalRepository.findByIdForUpdate(hospitalId)).thenReturn(Optional.of(hospital));
        when(admissionRepository.save(any(PatientAdmission.class))).thenAnswer(inv -> inv.getArgument(0));

        PatientAdmissionResponse response = admissionService.dischargePatient(admissionId);

        assertEquals(AdmissionStatus.DISCHARGED, response.getStatus());
        assertNotNull(response.getDischargedAt());
        assertEquals(11, hospital.getAvailableGeneralBeds()); // Returned bed from 10 to 11
    }

    @Test
    void testTransferPatient_ReleasesBedAndUpdatesStatus() {
        UUID admissionId = UUID.randomUUID();
        PatientAdmission admission = PatientAdmission.builder()
                .id(admissionId)
                .hospital(hospital)
                .incidentId(incidentId)
                .requiresIcu(true)
                .status(AdmissionStatus.ADMITTED)
                .build();

        when(admissionRepository.findById(admissionId)).thenReturn(Optional.of(admission));
        when(hospitalRepository.findByIdForUpdate(hospitalId)).thenReturn(Optional.of(hospital));
        when(admissionRepository.save(any(PatientAdmission.class))).thenAnswer(inv -> inv.getArgument(0));

        PatientAdmissionResponse response = admissionService.transferPatient(admissionId, "Transferred to Metro Trauma Center");

        assertEquals(AdmissionStatus.TRANSFERRED, response.getStatus());
        assertEquals(3, hospital.getAvailableIcuBeds()); // Returned ICU bed from 2 to 3
        assertTrue(response.getTriageNotes().contains("Transfer Notes: Transferred to Metro Trauma Center"));
    }

    @Test
    void testGetAdmissionsForHospital() {
        PatientAdmission pa1 = PatientAdmission.builder()
                .id(UUID.randomUUID())
                .hospital(hospital)
                .incidentId(incidentId)
                .triageLevel(TriageLevel.GREEN)
                .requiresIcu(false)
                .status(AdmissionStatus.ADMITTED)
                .build();

        when(admissionRepository.findByHospitalIdOrderByAdmittedAtDesc(hospitalId)).thenReturn(List.of(pa1));

        List<PatientAdmissionResponse> result = admissionService.getAdmissionsForHospital(hospitalId, false);

        assertEquals(1, result.size());
        assertEquals(TriageLevel.GREEN, result.get(0).getTriageLevel());
    }
}
