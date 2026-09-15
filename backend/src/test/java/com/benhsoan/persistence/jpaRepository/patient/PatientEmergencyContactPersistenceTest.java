package com.benhsoan.persistence.jpaRepository.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.persistence.entity.patient.PatientEntity;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@DisplayName("Patient Emergency Contact - JPA & Schema Persistence Integration Test (F-6)")
class PatientEmergencyContactPersistenceTest {

    @Autowired
    private JpaPatientRepository patientRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Instant NOW = Instant.parse("2026-09-15T00:00:00Z");

    @Test
    @DisplayName("Persists and reads patient with full cohesive triplet (including emergency_relationship)")
    void persistsAndReadsPatientWithFullCohesiveTriplet() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        PatientEntity entity = PatientEntity.builder()
                .id(id)
                .patientCode("BN-TEST-001")
                .fullName("Nguyễn Văn A")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender(Gender.MALE)
                .phone("0912345678")
                .emergencyContact("Nguyễn Văn B")
                .emergencyRelationship("Bố")
                .emergencyPhone("0987654321")
                .active(true)
                .createdAt(NOW)
                .updatedAt(NOW)
                .userId(userId)
                .createdBy(createdBy)
                .consentAgreed(true)
                .consentAgreedAt(NOW)
                .consentVersion("v1.0")
                .consentWithdrawn(false)
                .nonMedicalUseRestricted(false)
                .build();

        patientRepository.save(entity);

        PatientEntity loaded = patientRepository.findById(id).orElseThrow();
        assertNotNull(loaded);
        assertEquals("Nguyễn Văn B", loaded.getEmergencyContact());
        assertEquals("Bố", loaded.getEmergencyRelationship());
        assertEquals("0987654321", loaded.getEmergencyPhone());
    }

    @Test
    @DisplayName("Persists and reads patient with null emergency_relationship without schema constraint violation")
    void persistsAndReadsPatientWithNullEmergencyRelationship() {
        UUID id = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        PatientEntity entity = PatientEntity.builder()
                .id(id)
                .patientCode("BN-TEST-002")
                .fullName("Trần Thị C")
                .dateOfBirth(LocalDate.of(1995, 5, 10))
                .gender(Gender.FEMALE)
                .phone("0923456789")
                .emergencyContact(null)
                .emergencyRelationship(null)
                .emergencyPhone(null)
                .active(true)
                .createdAt(NOW)
                .updatedAt(NOW)
                .createdBy(createdBy)
                .consentAgreed(true)
                .consentAgreedAt(NOW)
                .consentVersion("v1.0")
                .consentWithdrawn(false)
                .nonMedicalUseRestricted(false)
                .build();

        patientRepository.save(entity);

        PatientEntity loaded = patientRepository.findById(id).orElseThrow();
        assertNotNull(loaded);
        assertNull(loaded.getEmergencyContact());
        assertNull(loaded.getEmergencyRelationship());
        assertNull(loaded.getEmergencyPhone());
    }

    @Test
    @DisplayName("Updates existing patient emergency_relationship and reads back")
    void updatesEmergencyRelationshipSuccessfully() {
        UUID id = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        PatientEntity entity = PatientEntity.builder()
                .id(id)
                .patientCode("BN-TEST-003")
                .fullName("Lê Văn D")
                .dateOfBirth(LocalDate.of(1988, 3, 15))
                .gender(Gender.MALE)
                .active(true)
                .createdAt(NOW)
                .updatedAt(NOW)
                .createdBy(createdBy)
                .consentAgreed(true)
                .consentAgreedAt(NOW)
                .consentVersion("v1.0")
                .consentWithdrawn(false)
                .nonMedicalUseRestricted(false)
                .build();

        patientRepository.save(entity);

        PatientEntity loaded = patientRepository.findById(id).orElseThrow();
        loaded.setEmergencyContact("Lê Thị E");
        loaded.setEmergencyRelationship("Vợ");
        loaded.setEmergencyPhone("0934567890");
        patientRepository.save(loaded);

        PatientEntity updated = patientRepository.findById(id).orElseThrow();
        assertEquals("Lê Thị E", updated.getEmergencyContact());
        assertEquals("Vợ", updated.getEmergencyRelationship());
        assertEquals("0934567890", updated.getEmergencyPhone());
    }

    @Test
    @DisplayName("Verifies V55 DDL execution on existing table structure without backfill modification")
    void verifiesV55DdlExecutionOnExistingTable() {
        jdbcTemplate.execute("CREATE TABLE test_patients_legacy (id VARCHAR(36) PRIMARY KEY, emergency_contact VARCHAR(100), emergency_phone VARCHAR(20))");
        jdbcTemplate.execute("INSERT INTO test_patients_legacy (id, emergency_contact, emergency_phone) VALUES ('p-1', 'Legacy Contact', '0912345678')");

        // Execute exact V55 DDL
        jdbcTemplate.execute("ALTER TABLE test_patients_legacy ADD COLUMN emergency_relationship VARCHAR(50) NULL");

        // Verify column exists and existing legacy records have NULL relationship
        var rows = jdbcTemplate.queryForList("SELECT id, emergency_contact, emergency_relationship, emergency_phone FROM test_patients_legacy WHERE id = 'p-1'");
        assertEquals(1, rows.size());
        assertEquals("Legacy Contact", rows.get(0).get("emergency_contact"));
        assertEquals("0912345678", rows.get(0).get("emergency_phone"));
        assertNull(rows.get(0).get("emergency_relationship"));
    }
}
