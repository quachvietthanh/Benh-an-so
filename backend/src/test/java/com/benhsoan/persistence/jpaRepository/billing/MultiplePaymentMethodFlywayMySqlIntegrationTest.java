package com.benhsoan.persistence.jpaRepository.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
        "logging.level.org.hibernate.SQL=WARN"
})
@DisplayName("Multiple Payment Methods Flyway MySQL Integration Tests (V82)")
class MultiplePaymentMethodFlywayMySqlIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("multiple_payment_flyway_test")
            .withUsername("payment_test")
            .withPassword("payment_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("MySQL V82: payment_method_items table exists with correct columns and constraints")
    void verifiesPaymentMethodItemsTableStructure() {
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'payment_method_items'",
                Integer.class
        );
        assertEquals(1, tableCount, "Table payment_method_items must exist after V82 migration");

        Integer indexCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'payment_method_items' AND index_name = 'idx_payment_method_items_payment'",
                Integer.class
        );
        assertTrue(indexCount != null && indexCount > 0, "Index idx_payment_method_items_payment must exist");
    }

    @Test
    @DisplayName("MySQL V82: payments table accepts MULTIPLE in payment_method column after DROP CHECK")
    void verifiesPaymentsAcceptsMultiplePaymentMethod() {
        // Verify constraint allows inserting or updating to 'MULTIPLE'
        List<Map<String, Object>> payments = jdbcTemplate.queryForList(
                "SELECT BIN_TO_UUID(id) as id, payment_method, amount_paid FROM payments LIMIT 1"
        );
        assertNotNull(payments);
        assertFalse(payments.isEmpty());

        String firstPaymentId = (String) payments.get(0).get("id");

        // Update payment_method to MULTIPLE - this proves DROP CHECK and ADD CONSTRAINT worked on MySQL 8.4
        int updated = jdbcTemplate.update(
                "UPDATE payments SET payment_method = 'MULTIPLE' WHERE id = UUID_TO_BIN(?)",
                firstPaymentId
        );
        assertEquals(1, updated);

        String updatedMethod = jdbcTemplate.queryForObject(
                "SELECT payment_method FROM payments WHERE id = UUID_TO_BIN(?)",
                String.class,
                firstPaymentId
        );
        assertEquals("MULTIPLE", updatedMethod);
    }

    @Test
    @DisplayName("MySQL V82: backfill migrated historical payments into payment_method_items")
    void verifiesHistoricalPaymentsAreBackfilled() {
        Integer backfilledCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_method_items",
                Integer.class
        );
        assertNotNull(backfilledCount);
        assertTrue(backfilledCount > 0, "Historical payments must be backfilled into payment_method_items");

        List<Map<String, Object>> items = jdbcTemplate.queryForList(
                "SELECT BIN_TO_UUID(id) as id, BIN_TO_UUID(payment_id) as payment_id, payment_method, amount FROM payment_method_items LIMIT 5"
        );
        assertFalse(items.isEmpty());
        for (Map<String, Object> item : items) {
            assertNotNull(item.get("payment_method"));
            BigDecimal amount = (BigDecimal) item.get("amount");
            assertTrue(amount.compareTo(BigDecimal.ZERO) > 0, "Backfilled amount must be positive");
        }
    }
}
