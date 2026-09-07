package com.benhsoan.persistence.jpaRepository.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.benhsoan.domain.patient.enums.AllergySeverity;
import com.benhsoan.persistence.entity.patient.PatientAllergyChangeLogEntity;
import com.benhsoan.persistence.entity.patient.PatientAllergyEntity;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class PatientAllergyPersistenceJpaIntegrationTest {

    @Autowired private JpaPatientAllergyRepository allergyRepository;
    @Autowired private JpaPatientAllergyChangeLogRepository changeLogRepository;

    private static final Instant NOW = Instant.parse("2026-09-07T08:00:00Z");

    @Test
    @DisplayName("Lưu và tìm kiếm dị ứng theo bệnh nhân và trạng thái active")
    void savesAndFindsAllergiesByPatientAndActive() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        PatientAllergyEntity activeAllergy = PatientAllergyEntity.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .allergenType("MEDICATION")
                .allergenName("Amoxicillin")
                .normalizedAllergenName("amoxicillin")
                .severity(AllergySeverity.MILD)
                .reaction("Ngứa")
                .notes("Ghi chú")
                .active(true)
                .createdBy(doctorId)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        PatientAllergyEntity inactiveAllergy = PatientAllergyEntity.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .allergenType("MEDICATION")
                .allergenName("Aspirin")
                .normalizedAllergenName("aspirin")
                .severity(AllergySeverity.MODERATE)
                .active(false)
                .createdBy(doctorId)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        allergyRepository.save(activeAllergy);
        allergyRepository.save(inactiveAllergy);

        List<PatientAllergyEntity> activeList = allergyRepository.findByPatientIdAndActiveTrue(patientId);
        assertEquals(1, activeList.size());
        assertEquals("Amoxicillin", activeList.get(0).getAllergenName());

        assertTrue(allergyRepository.existsByPatientIdAndNormalizedAllergenNameAndActiveTrue(patientId, "amoxicillin"));
        assertFalse(allergyRepository.existsByPatientIdAndNormalizedAllergenNameAndActiveTrue(patientId, "aspirin"));
        assertFalse(allergyRepository.existsByPatientIdAndNormalizedAllergenNameAndActiveTrue(patientId, "paracetamol"));

        assertFalse(allergyRepository.existsByPatientIdAndNormalizedAllergenNameAndActiveTrueAndIdNot(
                patientId, "amoxicillin", activeAllergy.getId()
        ));
        assertTrue(allergyRepository.existsByPatientIdAndNormalizedAllergenNameAndActiveTrueAndIdNot(
                patientId, "amoxicillin", UUID.randomUUID()
        ));
    }

    @Test
    @DisplayName("Lưu và tra cứu change log sắp xếp theo thời gian giảm dần")
    void savesAndRetrievesChangeLogsOrderedByChangedAtDesc() {
        UUID allergyId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        PatientAllergyChangeLogEntity log1 = PatientAllergyChangeLogEntity.builder()
                .id(UUID.randomUUID())
                .allergyId(allergyId)
                .patientId(patientId)
                .action("CREATE")
                .beforeData(null)
                .afterData("{\"allergenName\":\"Penicillin\",\"severity\":\"MILD\"}")
                .changeReason("Ghi nhận ban đầu")
                .changedBy(doctorId)
                .changedAt(NOW)
                .build();

        PatientAllergyChangeLogEntity log2 = PatientAllergyChangeLogEntity.builder()
                .id(UUID.randomUUID())
                .allergyId(allergyId)
                .patientId(patientId)
                .action("UPDATE")
                .beforeData("{\"allergenName\":\"Penicillin\",\"severity\":\"MILD\"}")
                .afterData("{\"allergenName\":\"Penicillin\",\"severity\":\"SEVERE\"}")
                .changeReason("Bổ sung mức độ nặng")
                .changedBy(doctorId)
                .changedAt(NOW.plusSeconds(300))
                .build();

        changeLogRepository.save(log1);
        changeLogRepository.save(log2);

        List<PatientAllergyChangeLogEntity> logs = changeLogRepository.findByAllergyIdOrderByChangedAtDesc(allergyId);
        assertEquals(2, logs.size());
        assertEquals("UPDATE", logs.get(0).getAction());
        assertEquals("CREATE", logs.get(1).getAction());
        assertNotNull(logs.get(0).getBeforeData());
        assertNotNull(logs.get(0).getAfterData());
    }
}
