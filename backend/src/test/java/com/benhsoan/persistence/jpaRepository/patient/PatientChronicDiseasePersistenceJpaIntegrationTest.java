package com.benhsoan.persistence.jpaRepository.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.benhsoan.persistence.entity.patient.PatientChronicDiseaseEntity;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class PatientChronicDiseasePersistenceJpaIntegrationTest {

    @Autowired private JpaPatientChronicDiseaseRepository repository;

    private static final Instant NOW = Instant.parse("2026-09-10T08:00:00Z");

    @Test
    @DisplayName("Saves and finds active chronic diseases by patient")
    void savesAndFindsByPatientAndActive() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID catalogId = UUID.randomUUID();

        PatientChronicDiseaseEntity active = PatientChronicDiseaseEntity.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .diagnosisCatalogId(catalogId)
                .yearDetected(2015)
                .notes("Đang điều trị")
                .active(true)
                .createdBy(doctorId)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
        PatientChronicDiseaseEntity inactive = PatientChronicDiseaseEntity.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .diagnosisCatalogId(UUID.randomUUID())
                .active(false)
                .createdBy(doctorId)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
        repository.save(active);
        repository.save(inactive);

        List<PatientChronicDiseaseEntity> result = repository.findByPatientIdAndActiveTrue(patientId);
        assertEquals(1, result.size());
        assertEquals(catalogId, result.get(0).getDiagnosisCatalogId());
        assertEquals(2015, result.get(0).getYearDetected());

        assertTrue(repository.existsByPatientIdAndDiagnosisCatalogIdAndActiveTrue(patientId, catalogId));
        assertFalse(repository.existsByPatientIdAndDiagnosisCatalogIdAndActiveTrue(patientId, UUID.randomUUID()));
    }

    @Test
    @DisplayName("Deactivated chronic disease is no longer returned by active query")
    void deactivatedChronicDiseaseExcludedFromActiveQuery() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID catalogId = UUID.randomUUID();

        PatientChronicDiseaseEntity entity = PatientChronicDiseaseEntity.builder()
                .id(UUID.randomUUID())
                .patientId(patientId)
                .diagnosisCatalogId(catalogId)
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
