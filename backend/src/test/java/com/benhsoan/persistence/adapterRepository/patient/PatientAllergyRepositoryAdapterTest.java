package com.benhsoan.persistence.adapterRepository.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.benhsoan.domain.patient.PatientAllergy;
import com.benhsoan.domain.patient.enums.AllergySeverity;
import com.benhsoan.domain.patient.exception.PatientAllergyAlreadyExistsException;
import com.benhsoan.persistence.entity.patient.PatientAllergyEntity;
import com.benhsoan.persistence.jpaRepository.patient.JpaPatientAllergyRepository;
import com.benhsoan.persistence.mapper.patient.PatientAllergyPersistenceMapper;

@ExtendWith(MockitoExtension.class)
class PatientAllergyRepositoryAdapterTest {

    @Mock
    private JpaPatientAllergyRepository jpaRepository;

    @Spy
    private PatientAllergyPersistenceMapper mapper = new PatientAllergyPersistenceMapper();

    @InjectMocks
    private PatientAllergyRepositoryAdapter adapter;

    private PatientAllergy testAllergy;

    @BeforeEach
    void setUp() {
        testAllergy = PatientAllergy.create(
                UUID.randomUUID(),
                "MEDICATION",
                "Amoxicillin",
                AllergySeverity.MILD,
                "Ngứa",
                "Ghi chú",
                UUID.randomUUID(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("save thành công khi dữ liệu hợp lệ")
    void save_success() {
        when(jpaRepository.saveAndFlush(any(PatientAllergyEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PatientAllergy saved = adapter.save(testAllergy);

        assertNotNull(saved);
        assertEquals("Amoxicillin", saved.getAllergenName());
    }

    @Test
    @DisplayName("N1: Chuyển đổi constraint uk_patient_active_allergen thành PatientAllergyAlreadyExistsException")
    void save_duplicateConstraint_throwsPatientAllergyAlreadyExistsException() {
        when(jpaRepository.saveAndFlush(any(PatientAllergyEntity.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry 'amoxicillin' for key 'uk_patient_active_allergen'"));

        assertThrows(PatientAllergyAlreadyExistsException.class, () -> adapter.save(testAllergy));
    }

    @Test
    @DisplayName("N1: Chuyển đổi duplicate entry với active_normalized_name thành PatientAllergyAlreadyExistsException")
    void save_duplicateNormalizedName_throwsPatientAllergyAlreadyExistsException() {
        when(jpaRepository.saveAndFlush(any(PatientAllergyEntity.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry for key 'active_normalized_name'"));

        assertThrows(PatientAllergyAlreadyExistsException.class, () -> adapter.save(testAllergy));
    }

    @Test
    @DisplayName("N1: Vi phạm khóa ngoại (foreign key) không bị biến thành duplicate exception mà rethrow DataIntegrityViolationException")
    void save_foreignKeyViolation_rethrowsDataIntegrityViolationException() {
        when(jpaRepository.saveAndFlush(any(PatientAllergyEntity.class)))
                .thenThrow(new DataIntegrityViolationException("Cannot add or update a child row: a foreign key constraint fails (fk_patient_allergies_created_by)"));

        assertThrows(DataIntegrityViolationException.class, () -> adapter.save(testAllergy));
    }
}
