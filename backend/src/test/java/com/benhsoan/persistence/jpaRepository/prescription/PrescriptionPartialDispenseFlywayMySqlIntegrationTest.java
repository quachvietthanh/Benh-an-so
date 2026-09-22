package com.benhsoan.persistence.jpaRepository.prescription;

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
@DisplayName("Prescription partial dispense Flyway MySQL Integration Tests (V68)")
class PrescriptionPartialDispenseFlywayMySqlIntegrationTest {

    private static final String SEEDED_PENDING_PRESCRIPTION_ID = "16200000-0000-0000-0000-000000000005";

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("partial_dispense_test")
            .withUsername("partial_dispense")
            .withPassword("partial_dispense");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired private JdbcTemplate jdbc;

    @Test
    @DisplayName("MySQL V68: chk_prescriptions_status chấp nhận PARTIALLY_DISPENSED")
    void checkConstraintAcceptsPartiallyDispensed() {
        Integer constraintCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.check_constraints "
                        + "WHERE constraint_schema = DATABASE() "
                        + "AND constraint_name = 'chk_prescriptions_status' "
                        + "AND check_clause LIKE '%PARTIALLY_DISPENSED%'",
                Integer.class
        );
        assertEquals(1, constraintCount,
                "chk_prescriptions_status must include PARTIALLY_DISPENSED");

        jdbc.update(
                "UPDATE prescriptions SET status = 'PARTIALLY_DISPENSED' WHERE id = UUID_TO_BIN(?)",
                SEEDED_PENDING_PRESCRIPTION_ID
        );

        String persisted = jdbc.queryForObject(
                "SELECT status FROM prescriptions WHERE id = UUID_TO_BIN(?)",
                String.class,
                SEEDED_PENDING_PRESCRIPTION_ID
        );
        assertEquals("PARTIALLY_DISPENSED", persisted,
                "PARTIALLY_DISPENSED must be persisted and read back");
    }
}
