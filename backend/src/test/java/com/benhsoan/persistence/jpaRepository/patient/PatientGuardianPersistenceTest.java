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
@DisplayName("Patient Guardian - JPA & Schema Persistence Integration Test (NCL-02-CN-008 / QTN-44)")
class PatientGuardianPersistenceTest {

    @Autowired
    private JpaPatientRepository patientRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Instant NOW = Instant.parse("2026-09-15T00:00:00Z");

    @Test
    @DisplayName("Persists and reads patient with full guardian fields and consent signer")
    void persistsAndReadsPatientWithGuardianDetails() {
        UUID id = UUID.randomUUID();
        UUID guardianUserId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        PatientEntity entity = PatientEntity.builder()
                .id(id)
                .patientCode("BN-CHILD-001")
                .fullName("Bé Nguyễn Văn Con")
                .dateOfBirth(LocalDate.of(2020, 5, 1))
                .gender(Gender.MALE)
                .guardianName("Nguyễn Văn Cha")
                .guardianRelationship("Bố")
                .guardianPhone("0912345678")
                .guardianIdentityNumber("079090001234")
                .guardianUserId(guardianUserId)
                .consentSignerName("Nguyễn Văn Cha")
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
        assertEquals("Nguyễn Văn Cha", loaded.getGuardianName());
        assertEquals("Bố", loaded.getGuardianRelationship());
        assertEquals("0912345678", loaded.getGuardianPhone());
        assertEquals("079090001234", loaded.getGuardianIdentityNumber());
        assertEquals(guardianUserId, loaded.getGuardianUserId());
        assertEquals("Nguyễn Văn Cha", loaded.getConsentSignerName());
    }

    @Test
    @DisplayName("Persists and reads adult patient with null guardian fields")
    void persistsAndReadsPatientWithNullGuardianFields() {
        UUID id = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        PatientEntity entity = PatientEntity.builder()
                .id(id)
                .patientCode("BN-ADULT-001")
                .fullName("Trần Thị Lớn")
                .dateOfBirth(LocalDate.of(1995, 1, 1))
                .gender(Gender.FEMALE)
                .consentSignerName("Trần Thị Lớn")
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
        assertNull(loaded.getGuardianName());
        assertNull(loaded.getGuardianRelationship());
        assertNull(loaded.getGuardianPhone());
        assertNull(loaded.getGuardianIdentityNumber());
        assertNull(loaded.getGuardianUserId());
        assertEquals("Trần Thị Lớn", loaded.getConsentSignerName());
    }

    @Test
    @DisplayName("Raw schema contains guardian columns")
    void rawSchemaContainsGuardianColumns() {
        UUID id = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();

        PatientEntity entity = PatientEntity.builder()
                .id(id)
                .patientCode("BN-SCHEMA-001")
                .fullName("Test Schema")
                .dateOfBirth(LocalDate.of(2022, 1, 1))
                .gender(Gender.MALE)
                .guardianName("Giám Hộ A")
                .guardianRelationship("Mẹ")
                .guardianPhone("0988888888")
                .consentSignerName("Giám Hộ A")
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
        patientRepository.flush();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(1) FROM patients WHERE id = ? AND guardian_name = 'Giám Hộ A' AND guardian_relationship = 'Mẹ'",
                Integer.class,
                id
        );
        assertEquals(1, count);
    }

    @Test
    @DisplayName("Verifies V56 DDL execution on existing table structure without backfill modification")
    void verifiesV56DdlExecutionOnExistingTable() {
        jdbcTemplate.execute("CREATE TABLE test_patients_v56 (id VARCHAR(36) PRIMARY KEY, full_name VARCHAR(100))");
        jdbcTemplate.execute("INSERT INTO test_patients_v56 (id, full_name) VALUES ('p-v56-1', 'Legacy Patient')");

        // Execute exact V56 DDL columns
        jdbcTemplate.execute("ALTER TABLE test_patients_v56 ADD COLUMN guardian_name VARCHAR(100) NULL");
        jdbcTemplate.execute("ALTER TABLE test_patients_v56 ADD COLUMN guardian_relationship VARCHAR(50) NULL");
        jdbcTemplate.execute("ALTER TABLE test_patients_v56 ADD COLUMN guardian_phone VARCHAR(20) NULL");
        jdbcTemplate.execute("ALTER TABLE test_patients_v56 ADD COLUMN guardian_identity_number VARCHAR(20) NULL");
        jdbcTemplate.execute("ALTER TABLE test_patients_v56 ADD COLUMN guardian_user_id BINARY(16) NULL");
        jdbcTemplate.execute("ALTER TABLE test_patients_v56 ADD COLUMN consent_signer_name VARCHAR(100) NULL");

        var rows = jdbcTemplate.queryForList("SELECT * FROM test_patients_v56 WHERE id = 'p-v56-1'");
        assertEquals(1, rows.size());
        assertEquals("Legacy Patient", rows.get(0).get("full_name"));
        assertNull(rows.get(0).get("guardian_name"));
        assertNull(rows.get(0).get("guardian_relationship"));
        assertNull(rows.get(0).get("guardian_phone"));
        assertNull(rows.get(0).get("guardian_identity_number"));
        assertNull(rows.get(0).get("guardian_user_id"));
        assertNull(rows.get(0).get("consent_signer_name"));
    }
}
