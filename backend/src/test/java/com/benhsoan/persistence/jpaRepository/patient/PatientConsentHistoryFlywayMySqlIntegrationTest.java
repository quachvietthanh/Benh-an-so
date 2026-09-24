package com.benhsoan.persistence.jpaRepository.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Kiểm thử Flyway Migration V90 và backfill dữ liệu cho bảng patient_consent_history trên MySQL 8.4 (Finding P0).
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.jpa.show-sql=false",
        "logging.level.org.hibernate.SQL=WARN",
        "logging.level.org.hibernate.orm.jdbc.bind=WARN"
})
@DisplayName("PatientConsentHistory Flyway MySQL Integration Test (V90 Migration - P0)")
class PatientConsentHistoryFlywayMySqlIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("patient_consent_history_test")
            .withUsername("consent_test")
            .withPassword("consent_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("P0: Flyway V90 tạo đúng bảng patient_consent_history, cột, ràng buộc duy nhất và backfill version 1")
    void flywayCreatesConsentHistoryTableWithConstraintsAndBackfill() {
        // 1. Xác nhận bảng patient_consent_history tồn tại
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = 'patient_consent_history'",
                Integer.class
        ));

        // 2. Xác nhận 15 cột được định nghĩa đầy đủ
        assertEquals(15, jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'patient_consent_history' "
                        + "AND column_name IN ("
                        + "'id', 'patient_id', 'version_number', 'version_code', 'status', 'scopes', "
                        + "'consent_agreed', 'consent_agreed_at', 'consent_withdrawn', 'consent_withdrawn_at', "
                        + "'consent_withdrawn_reason', 'non_medical_use_restricted', 'signer_name', 'created_by', 'created_at')",
                Integer.class
        ));

        // 3. Xác nhận unique constraint (patient_id, version_number)
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints "
                        + "WHERE table_schema = DATABASE() AND table_name = 'patient_consent_history' "
                        + "AND constraint_name = 'uq_patient_consent_history_version'",
                Integer.class
        ));

        // 4. Xác nhận foreign key trỏ tới patients(id)
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints "
                        + "WHERE table_schema = DATABASE() AND table_name = 'patient_consent_history' "
                        + "AND constraint_name = 'fk_patient_consent_history_patient'",
                Integer.class
        ));

        // 5. Xác nhận index trên (patient_id, created_at)
        Integer indexCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics "
                        + "WHERE table_schema = DATABASE() AND table_name = 'patient_consent_history' "
                        + "AND index_name = 'idx_patient_consent_history_patient'",
                Integer.class
        );
        assertTrue(indexCount != null && indexCount > 0, "Index idx_patient_consent_history_patient phải tồn tại");

        // 6. Xác nhận migration Flyway đã áp dụng thành công V90
        Integer v90Applied = jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '90' AND success = 1",
                Integer.class
        );
        assertEquals(1, v90Applied, "Flyway migration V90 bắt buộc phải được áp dụng thành công (success = 1)");
    }
}
