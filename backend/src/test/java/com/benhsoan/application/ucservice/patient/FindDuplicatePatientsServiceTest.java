package com.benhsoan.application.ucservice.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.port.dto.result.patient.DuplicatePatientGroupResult;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindDuplicatePatientsService - Unit Tests (NCL-02-CN-006 / TC-01)")
class FindDuplicatePatientsServiceTest {

    @Mock private PatientRepository patientRepository;

    private FindDuplicatePatientsService service;
    private final UUID operatorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new FindDuplicatePatientsService(patientRepository, new PatientResultMapper());
    }

    @Test
    @DisplayName("Nhóm chính xác các hồ sơ trùng tên, ngày sinh, số điện thoại thành từng nhóm (>= 2 hồ sơ)")
    void groupsSuspectedDuplicatesCorrectly() {
        LocalDate dob1 = LocalDate.of(1990, 1, 1);
        Patient p1 = Patient.create(
                "BN000001", "Nguyễn Văn A", dob1, Gender.MALE,
                "0901234567", null, "123 Street", null, null, BloodType.UNKNOWN,
                null, null, null, null, null, null, null, null, null,
                true, "v1.0", operatorId
        );
        p1.setIdForTest(UUID.randomUUID());

        Patient p2 = Patient.create(
                "BN000002", "NGUYEN VAN A", dob1, Gender.MALE,
                "0901234567", null, "456 Street", null, null, BloodType.UNKNOWN,
                null, null, null, null, null, null, null, null, null,
                true, "v1.0", operatorId
        );
        p2.setIdForTest(UUID.randomUUID());

        // Single patient without duplicate (should be excluded from groups)
        Patient p3 = Patient.create(
                "BN000003", "Trần Thị B", LocalDate.of(1995, 5, 5), Gender.FEMALE,
                "0987654321", null, "789 Street", null, null, BloodType.UNKNOWN,
                null, null, null, null, null, null, null, null, null,
                true, "v1.0", operatorId
        );
        p3.setIdForTest(UUID.randomUUID());

        when(patientRepository.findSuspectedDuplicates()).thenReturn(List.of(p1, p2, p3));

        List<DuplicatePatientGroupResult> results = service.findDuplicates();

        assertNotNull(results);
        assertEquals(1, results.size(), "Chỉ có 1 nhóm có >= 2 hồ sơ trùng");
        DuplicatePatientGroupResult group = results.get(0);
        assertEquals("Nguyễn Văn A", group.fullName());
        assertEquals(dob1, group.dateOfBirth());
        assertEquals("0901234567", group.phone());
        assertEquals(2, group.candidates().size());
    }
}
