package com.benhsoan.application.ucservice.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

class PatientImportDuplicateDetectorTest {

    private PatientRepository patientRepository;
    private PatientImportDuplicateDetector detector;

    @BeforeEach
    void setUp() {
        patientRepository = mock(PatientRepository.class);
        detector = new PatientImportDuplicateDetector(patientRepository);
    }

    @Test
    @DisplayName("Should detect internal duplicates in the same file by CCCD")
    void shouldDetectInternalDuplicateByCccd() {
        ValidatedPatientRowDto row1 = ValidatedPatientRowDto.builder()
                .rowNumber(2)
                .fullName("Nguyễn Văn A")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .identityNumber("001090123456")
                .build();

        ValidatedPatientRowDto row2 = ValidatedPatientRowDto.builder()
                .rowNumber(3)
                .fullName("Nguyễn Văn B")
                .dateOfBirth(LocalDate.of(1992, 2, 2))
                .gender(Gender.MALE)
                .identityNumber("001090123456") // Same CCCD
                .build();

        var result = detector.detectDuplicates(List.of(row1, row2));

        assertThat(result.nonDuplicateRows()).hasSize(1);
        assertThat(result.nonDuplicateRows().get(0).getRowNumber()).isEqualTo(2);
        assertThat(result.suspectedDuplicates()).hasSize(1);
        assertThat(result.suspectedDuplicates().get(0).rowNumber()).isEqualTo(3);
        assertThat(result.suspectedDuplicates().get(0).duplicateReason()).contains("Trùng số CCCD/CMND");
    }

    @Test
    @DisplayName("Should detect database duplicate by CCCD")
    void shouldDetectDatabaseDuplicateByCccd() {
        ValidatedPatientRowDto row = ValidatedPatientRowDto.builder()
                .rowNumber(2)
                .fullName("Trần Thị C")
                .dateOfBirth(LocalDate.of(1985, 5, 5))
                .gender(Gender.FEMALE)
                .identityNumber("001085999999")
                .build();

        when(patientRepository.existsByIdentityNumber("001085999999")).thenReturn(true);

        var result = detector.detectDuplicates(List.of(row));

        assertThat(result.nonDuplicateRows()).isEmpty();
        assertThat(result.suspectedDuplicates()).hasSize(1);
        assertThat(result.suspectedDuplicates().get(0).duplicateReason()).contains("Số CCCD/CMND đã tồn tại trên hệ thống");
    }

    @Test
    @DisplayName("Should detect database duplicate by Name + DOB + Phone")
    void shouldDetectDatabaseDuplicateByNameDobPhone() {
        ValidatedPatientRowDto row = ValidatedPatientRowDto.builder()
                .rowNumber(2)
                .fullName("Lê Hoàng Nam")
                .dateOfBirth(LocalDate.of(1991, 10, 20))
                .gender(Gender.MALE)
                .phone("0903111222")
                .build();

        Patient existingPatient = mock(Patient.class);
        when(existingPatient.getId()).thenReturn(UUID.randomUUID());
        when(existingPatient.getPatientCode()).thenReturn("BN-000123");
        when(existingPatient.getFullName()).thenReturn("Lê Hoàng Nam");
        when(existingPatient.getDateOfBirth()).thenReturn(LocalDate.of(1991, 10, 20));
        when(existingPatient.isMerged()).thenReturn(false);

        when(patientRepository.findAllByPhone("0903111222")).thenReturn(List.of(existingPatient));

        var result = detector.detectDuplicates(List.of(row));

        assertThat(result.nonDuplicateRows()).isEmpty();
        assertThat(result.suspectedDuplicates()).hasSize(1);
        assertThat(result.suspectedDuplicates().get(0).matchedExistingPatientCode()).isEqualTo("BN-000123");
    }
}
