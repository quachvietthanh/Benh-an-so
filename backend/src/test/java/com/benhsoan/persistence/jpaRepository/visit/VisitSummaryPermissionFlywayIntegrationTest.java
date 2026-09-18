package com.benhsoan.persistence.jpaRepository.visit;

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
class VisitSummaryPermissionFlywayIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("visit_summary_permission_test")
            .withUsername("visit_test")
            .withPassword("visit_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired private JdbcTemplate jdbc;

    @Test
    void flywayCreatesVisitSummaryPrintPermissionAndLinksExpectedRoles() {
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM permissions WHERE code = 'VISIT_SUMMARY_PRINT' AND module = 'CLINICAL'",
                Integer.class
        ));

        assertEquals(4, jdbc.queryForObject(
                "SELECT COUNT(*) FROM role_permissions rp "
                        + "JOIN roles r ON r.id = rp.role_id "
                        + "JOIN permissions p ON p.id = rp.permission_id "
                        + "WHERE p.code = 'VISIT_SUMMARY_PRINT' "
                        + "AND r.name IN ('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'MANAGER')",
                Integer.class
        ));

        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM role_permissions rp "
                        + "JOIN roles r ON r.id = rp.role_id "
                        + "JOIN permissions p ON p.id = rp.permission_id "
                        + "WHERE p.code = 'VISIT_SUMMARY_PRINT' "
                        + "AND r.name NOT IN ('ADMIN', 'DOCTOR', 'RECEPTIONIST', 'MANAGER')",
                Integer.class
        ));
    }
}
