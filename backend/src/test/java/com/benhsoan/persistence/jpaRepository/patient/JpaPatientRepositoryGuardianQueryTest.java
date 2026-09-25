package com.benhsoan.persistence.jpaRepository.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.benhsoan.domain.patient.enums.BloodType;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.patient.enums.PatientStatus;
import com.benhsoan.persistence.entity.patient.PatientEntity;

import jakarta.persistence.EntityManager;

/**
 * NCL-14-CN-010 / QTN-33: the guardian lookup queries must apply the patient lifecycle rule in
 * SQL, otherwise a merged or deactivated dependent would remain selectable for new activity and
 * the exclusion could not be enforced by the service that consumes the query.
 *
 * <p>Flyway is disabled for this slice test (project convention for H2 repository tests); the
 * schema is generated from the entities, which is what validates the JPQL against the real
 * entity model.</p>
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:guardian-query-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DisplayName("JpaPatientRepository guardian queries - NCL-14-CN-010")
class JpaPatientRepositoryGuardianQueryTest {

    private static final Instant NOW = Instant.parse("2026-09-25T03:00:00Z");
    private static final LocalDate MINOR_DOB = LocalDate.of(2015, 5, 10);

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JpaPatientRepository jpaRepository;

    private int codeSequence = 0;

    private UUID persistDependent(
            UUID guardianUserId,
            UUID portalUserId,
            boolean active,
            PatientStatus status
    ) {
        UUID id = UUID.randomUUID();
        codeSequence++;
        entityManager.persist(PatientEntity.builder()
                .id(id)
                .patientCode(String.format("BNQ%05d", codeSequence))
                .fullName("Con " + codeSequence)
                .dateOfBirth(MINOR_DOB)
                .gender(Gender.MALE)
                .bloodType(BloodType.O_POSITIVE)
                .guardianName("Cha")
                .guardianRelationship("Bo")
                .guardianUserId(guardianUserId)
                .active(active)
                .status(status)
                .createdAt(NOW)
                .updatedAt(NOW)
                .userId(portalUserId)
                .createdBy(UUID.randomUUID())
                .build());
        return id;
    }

    private void flush() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("chi tra ve ho so phu thuoc ACTIVE")
    void returnsOnlyActiveDependents() {
        UUID guardianUserId = UUID.randomUUID();
        UUID activeId = persistDependent(guardianUserId, UUID.randomUUID(), true, PatientStatus.ACTIVE);
        persistDependent(guardianUserId, UUID.randomUUID(), false, PatientStatus.INACTIVE);
        persistDependent(guardianUserId, UUID.randomUUID(), false, PatientStatus.MERGED);
        flush();

        List<PatientEntity> results = jpaRepository.findValidDependentsByGuardianUserId(guardianUserId);

        assertEquals(1, results.size());
        assertEquals(activeId, results.get(0).getId());
    }

    @Test
    @DisplayName("sap xep theo ten roi den id")
    void ordersByNameThenId() {
        UUID guardianUserId = UUID.randomUUID();
        persistDependent(guardianUserId, UUID.randomUUID(), true, PatientStatus.ACTIVE);
        persistDependent(guardianUserId, UUID.randomUUID(), true, PatientStatus.ACTIVE);
        flush();

        List<PatientEntity> results = jpaRepository.findValidDependentsByGuardianUserId(guardianUserId);

        assertEquals(2, results.size());
        assertTrue(results.get(0).getFullName().compareToIgnoreCase(results.get(1).getFullName()) <= 0);
    }

    @Test
    @DisplayName("findValidDependentByGuardianUserIdAndId loai ho so khong hop le")
    void singleLookupExcludesInvalidDependents() {
        UUID guardianUserId = UUID.randomUUID();
        UUID mergedId = persistDependent(guardianUserId, UUID.randomUUID(), false, PatientStatus.MERGED);
        UUID activeId = persistDependent(guardianUserId, UUID.randomUUID(), true, PatientStatus.ACTIVE);
        flush();

        assertTrue(jpaRepository.findValidDependentByGuardianUserIdAndId(guardianUserId, mergedId).isEmpty());
        assertTrue(jpaRepository.findValidDependentByGuardianUserIdAndId(guardianUserId, activeId).isPresent());
    }

    @Test
    @DisplayName("findValidDependentByGuardianUserIdAndId khong vuot pham vi nguoi giam ho")
    void singleLookupRequiresMatchingGuardian() {
        UUID guardianUserId = UUID.randomUUID();
        UUID otherGuardianUserId = UUID.randomUUID();
        UUID dependentId = persistDependent(guardianUserId, UUID.randomUUID(), true, PatientStatus.ACTIVE);
        flush();

        assertTrue(jpaRepository
                .findValidDependentByGuardianUserIdAndId(otherGuardianUserId, dependentId).isEmpty());
    }

    @Test
    @DisplayName("sweep TC-03 chi lay ho so con lien ket giam ho va hop le")
    void sweepQueryReturnsOnlyLinkedValidProfiles() {
        UUID guardianUserId = UUID.randomUUID();
        UUID linkedId = persistDependent(guardianUserId, UUID.randomUUID(), true, PatientStatus.ACTIVE);
        persistDependent(null, UUID.randomUUID(), true, PatientStatus.ACTIVE);
        persistDependent(guardianUserId, UUID.randomUUID(), false, PatientStatus.INACTIVE);
        flush();

        List<PatientEntity> results = jpaRepository.findGuardianLinkedProfiles();

        assertEquals(1, results.size());
        assertEquals(linkedId, results.get(0).getId());
    }
}
