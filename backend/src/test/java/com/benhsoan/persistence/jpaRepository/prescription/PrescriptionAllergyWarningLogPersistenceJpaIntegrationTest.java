package com.benhsoan.persistence.jpaRepository.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.benhsoan.domain.patient.enums.AllergySeverity;
import com.benhsoan.persistence.entity.prescription.PrescriptionAllergyWarningLogEntity;
import com.benhsoan.port.dto.command.prescription.SearchPrescriptionAllergyWarningLogsQuery;

@DataJpaTest(properties = {
                "spring.flyway.enabled=false",
                "spring.sql.init.mode=never",
                "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DisplayName("PrescriptionAllergyWarningLog Persistence JPA Integration Tests")
class PrescriptionAllergyWarningLogPersistenceJpaIntegrationTest {

        @Autowired
        private JpaPrescriptionAllergyWarningLogRepository repository;

        private static final Instant T1 = Instant.parse("2026-09-01T08:00:00Z");
        private static final Instant T2 = Instant.parse("2026-09-02T08:00:00Z");
        private static final Instant T3 = Instant.parse("2026-09-03T08:00:00Z");

        @Test
        @DisplayName("Saves and finds allergy warning logs by prescription ID ordered by createdAt ASC")
        void savesAndFindsByPrescriptionId() {
                UUID prescriptionId = UUID.randomUUID();
                UUID patientId = UUID.randomUUID();
                UUID doctorId = UUID.randomUUID();

                PrescriptionAllergyWarningLogEntity log1 = buildEntity(
                                UUID.randomUUID(), prescriptionId, patientId, doctorId, "Amoxicillin", T1);
                PrescriptionAllergyWarningLogEntity log2 = buildEntity(
                                UUID.randomUUID(), prescriptionId, patientId, doctorId, "Aspirin", T2);

                repository.save(log2);
                repository.save(log1);

                List<PrescriptionAllergyWarningLogEntity> found = repository
                                .findByPrescriptionIdOrderByCreatedAtAsc(prescriptionId);

                assertEquals(2, found.size());
                assertEquals("Amoxicillin", found.get(0).getAllergenName());
                assertEquals("Aspirin", found.get(1).getAllergenName());
        }

        @Test
        @DisplayName("Deletes allergy warning logs by prescription IDs")
        void deletesByPrescriptionIdIn() {
                UUID prescriptionId1 = UUID.randomUUID();
                UUID prescriptionId2 = UUID.randomUUID();
                UUID otherPrescriptionId = UUID.randomUUID();

                repository.save(buildEntity(UUID.randomUUID(), prescriptionId1, UUID.randomUUID(), UUID.randomUUID(),
                                "Allergen 1", T1));
                repository.save(buildEntity(UUID.randomUUID(), prescriptionId2, UUID.randomUUID(), UUID.randomUUID(),
                                "Allergen 2", T2));
                PrescriptionAllergyWarningLogEntity retained = repository.save(
                                buildEntity(UUID.randomUUID(), otherPrescriptionId, UUID.randomUUID(),
                                                UUID.randomUUID(), "Retained", T3));

                repository.deleteByPrescriptionIdIn(List.of(prescriptionId1, prescriptionId2));
                repository.flush();

                List<PrescriptionAllergyWarningLogEntity> remaining = repository.findAll();
                assertEquals(1, remaining.size());
                assertEquals(retained.getId(), remaining.getFirst().getId());
        }

        @Test
        @DisplayName("Filters dynamic specification by doctorId, patientId, and time range")
        void filtersBySpecificationCriteria() {
                UUID doctorA = UUID.randomUUID();
                UUID doctorB = UUID.randomUUID();
                UUID patientA = UUID.randomUUID();
                UUID patientB = UUID.randomUUID();

                // Matching record
                repository.save(buildEntity(UUID.randomUUID(), UUID.randomUUID(), patientA, doctorA, "Target", T2));
                // Different doctor
                repository.save(buildEntity(UUID.randomUUID(), UUID.randomUUID(), patientA, doctorB, "DiffDoctor", T2));
                // Different patient
                repository.save(buildEntity(UUID.randomUUID(), UUID.randomUUID(), patientB, doctorA, "DiffPatient",
                                T2));
                // Outside time range
                repository.save(buildEntity(UUID.randomUUID(), UUID.randomUUID(), patientA, doctorA, "OldRecord", T1));

                SearchPrescriptionAllergyWarningLogsQuery query = new SearchPrescriptionAllergyWarningLogsQuery(
                                doctorA,
                                patientA,
                                Instant.parse("2026-09-01T12:00:00Z"),
                                Instant.parse("2026-09-02T23:59:59Z"),
                                0,
                                10);

                Page<PrescriptionAllergyWarningLogEntity> resultPage = repository.findAll(
                                PrescriptionAllergyWarningLogSpecification.build(query),
                                PageRequest.of(query.page(), query.size(),
                                                Sort.by(Sort.Order.desc("handledAt"), Sort.Order.desc("id"))));

                assertNotNull(resultPage);
                assertEquals(1, resultPage.getTotalElements());
                assertEquals("Target", resultPage.getContent().getFirst().getAllergenName());
        }

        private PrescriptionAllergyWarningLogEntity buildEntity(
                        UUID id, UUID prescriptionId, UUID patientId, UUID doctorId, String allergenName,
                        Instant handledAt) {
                return PrescriptionAllergyWarningLogEntity.builder()
                                .id(id)
                                .prescriptionId(prescriptionId)
                                .patientId(patientId)
                                .allergyId(UUID.randomUUID())
                                .medicineId(UUID.randomUUID())
                                .activeIngredient("Active ingredient")
                                .allergenName(allergenName)
                                .severity(AllergySeverity.SEVERE)
                                .reaction("Anaphylaxis")
                                .overrideReason("Clinical judgment overriding allergy warning")
                                .handledBy(doctorId)
                                .handledAt(handledAt)
                                .createdAt(handledAt)
                                .build();
        }
}
