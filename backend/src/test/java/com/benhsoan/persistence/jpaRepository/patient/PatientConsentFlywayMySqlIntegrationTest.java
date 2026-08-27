package com.benhsoan.persistence.jpaRepository.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
class PatientConsentFlywayMySqlIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("patient_consent_test")
            .withUsername("patient_consent_test")
            .withPassword("patient_consent_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired private JdbcTemplate jdbc;

    @Test
    void flywayCreatesConsentColumnsPreservesHistoricalPatientsAndGrantsOnlyRequiredRoles() {
        assertEquals(7, jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = 'patients' "
                        + "AND column_name IN ('consent_agreed', 'consent_agreed_at', 'consent_version', "
                        + "'consent_withdrawn', 'consent_withdrawn_at', 'consent_withdrawn_reason', "
                        + "'non_medical_use_restricted')",
                Integer.class
        ));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM patients "
                        + "WHERE consent_agreed <> FALSE OR consent_agreed_at IS NOT NULL "
                        + "OR consent_version IS NOT NULL OR consent_withdrawn <> FALSE "
                        + "OR non_medical_use_restricted <> FALSE",
                Integer.class
        ));
        assertEquals(2, jdbc.queryForObject(
                "SELECT COUNT(*) FROM role_permissions rp "
                        + "JOIN permissions p ON p.id = rp.permission_id "
                        + "JOIN roles r ON r.id = rp.role_id "
                        + "WHERE p.code = 'PATIENT_CONSENT_UPDATE' "
                        + "AND r.name IN ('ADMIN', 'RECEPTIONIST')",
                Integer.class
        ));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM role_permissions rp "
                        + "JOIN permissions p ON p.id = rp.permission_id "
                        + "JOIN roles r ON r.id = rp.role_id "
                        + "WHERE p.code = 'PATIENT_CONSENT_UPDATE' "
                        + "AND r.name NOT IN ('ADMIN', 'RECEPTIONIST')",
                Integer.class
        ));
    }
}
