package com.benhsoan.persistence.jpaRepository.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.benhsoan.persistence.entity.patient.PatientFamilyHistoryEntity;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class PatientFamilyHistoryPersistenceJpaIntegrationTest {

    @Autowired private JpaPatientFamilyHistoryRepository repository;

    private static final Instant NOW = Instant.parse("2026-09-10T08:00:00Z");

    @Test
    @DisplayName("Saves and finds active family history by patient")
    void savesAndFindsByPatientAndActive() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID catalogId = UUID.randomUUID();

        PatientFamilyHistoryEntity active = PatientFamilyHistoryEntity.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .relationship("Bố")
                .diagnosisCatalogId(catalogId)
                .notes("Đã mổ")
                .active(true)
                .createdBy(doctorId)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
        PatientFamilyHistoryEntity inactive = PatientFamilyHistoryEntity.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .relationship("Mẹ")
                .diagnosisCatalogId(UUID.randomUUID())
                .active(false)
                .createdBy(doctorId)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
        repository.save(active);
        repository.save(inactive);

        List<PatientFamilyHistoryEntity> result = repository.findByPatientIdAndActiveTrue(patientId);
        assertEquals(1, result.size());
        assertEquals("Bố", result.get(0).getRelationship());
        assertEquals(catalogId, result.get(0).getDiagnosisCatalogId());
    }

    @Test
    @DisplayName("Deactivated family history is no longer returned by active query")
    void deactivatedFamilyHistoryExcludedFromActiveQuery() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        PatientFamilyHistoryEntity entity = PatientFamilyHistoryEntity.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .relationship("Bố")
                .diagnosisCatalogId(UUID.randomUUID())
                .active(true)
                .createdBy(doctorId)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
        entity = repository.save(entity);

        entity.setActive(false);
        entity.setUpdatedBy(doctorId);
        entity.setUpdatedAt(NOW.plusSeconds(60));
        repository.save(entity);

        assertTrue(repository.findByPatientIdAndActiveTrue(patientId).isEmpty());
    }
}
