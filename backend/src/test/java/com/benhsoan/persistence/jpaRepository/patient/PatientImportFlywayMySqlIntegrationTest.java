package com.benhsoan.persistence.jpaRepository.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.jpa.show-sql=false",
        "logging.level.org.hibernate.SQL=WARN",
        "logging.level.org.hibernate.orm.jdbc.bind=WARN"
})
@DisplayName("Patient Import Flyway MySQL Schema and Permission Integration Test (NCL-02-CN-010 / V77)")
class PatientImportFlywayMySqlIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("patient_import_test")
            .withUsername("patient_import_test")
            .withPassword("patient_import_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Flyway V77 creates patient_import_logs tables and seeds PATIENT_IMPORT permission for ADMIN and RECEPTIONIST")
    void flywayCreatesImportTablesAndSeedsPermissions() {
        // Verify patient_import_logs columns
        assertEquals(10, jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'patient_import_logs' "
                        + "AND column_name IN ('id', 'file_name', 'file_size', 'total_rows', 'success_rows', 'error_rows', 'duplicate_rows', 'status', 'imported_by', 'created_at')",
                Integer.class
        ));

        // Verify patient_import_log_errors columns
        assertEquals(6, jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'patient_import_log_errors' "
                        + "AND column_name IN ('id', 'import_log_id', 'row_number', 'error_field', 'error_message', 'raw_data')",
                Integer.class
        ));

        // Verify PATIENT_IMPORT permission exists
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM permissions WHERE code = 'PATIENT_IMPORT'",
                Integer.class
        ));

        // Verify PATIENT_IMPORT granted to ADMIN and RECEPTIONIST
        assertEquals(2, jdbc.queryForObject(
                "SELECT COUNT(*) FROM role_permissions rp "
                        + "JOIN permissions p ON p.id = rp.permission_id "
                        + "JOIN roles r ON r.id = rp.role_id "
                        + "WHERE p.code = 'PATIENT_IMPORT' AND r.name IN ('ADMIN', 'RECEPTIONIST')",
                Integer.class
        ));
    }
}
