package com.benhsoan.application.ucservice.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.benhsoan.domain.billing.enums.InvoiceLineType;
import com.benhsoan.domain.billing.enums.PaymentMethod;
import com.benhsoan.port.dto.command.billing.CreateInvoiceCommand;
import com.benhsoan.port.dto.command.billing.RecordPaymentCommand;
import com.benhsoan.port.outbound.generator.InvoiceCodeGenerator;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.jpa.show-sql=false",
        "logging.level.org.hibernate.SQL=WARN",
        "logging.level.org.hibernate.orm.jdbc.bind=WARN"
})
class ClinicalServiceBillingMySqlIntegrationTest {

    private static final UUID ADMIN_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final UUID CLINICAL_SERVICE_ID = UUID.fromString("f0000000-0000-0000-0000-000000000010");

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("clinical_service_billing_test")
            .withUsername("billing_test")
            .withPassword("billing_test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired private RecordPaymentService recordPaymentService;
    @Autowired private CreateInvoiceService createInvoiceService;
    @Autowired private JdbcTemplate jdbc;

    @MockitoBean private CurrentUserPort currentUserPort;
    @MockitoBean private ClockPort clockPort;
    @MockitoBean private InvoiceCodeGenerator invoiceCodeGenerator;

    private final AtomicReference<Instant> now = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(ADMIN_ID);
        when(clockPort.now()).thenAnswer(invocation -> now.get());
        when(invoiceCodeGenerator.generate()).thenReturn("HD-E2E-" + UUID.randomUUID().toString().substring(0, 8));
    }

    @Test
    void snapshotsCompletedClinicalServiceFeeAndInvoicesItAfterMasterPriceChanges() {
        Fixture fixture = seedCompletedClinicalOrder();
        now.set(Instant.parse("2026-08-18T08:00:00Z"));

        var payment = recordPaymentService.record(new RecordPaymentCommand(
                fixture.visitId(),
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                new BigDecimal("195000"),
                PaymentMethod.CASH
        ));

        assertEquals(new BigDecimal("95000.00"), payment.serviceFee());
        assertEquals(new BigDecimal("95000.00"), jdbc.queryForObject(
                "SELECT amount FROM payment_service_fees WHERE payment_id = UUID_TO_BIN(?)",
                BigDecimal.class,
                payment.id().toString()
        ));

        jdbc.update("""
                INSERT INTO service_price (
                    id, service_catalog_id, price, effective_from, created_at, created_by
                ) VALUES (
                    UUID_TO_BIN(?), UUID_TO_BIN(?), ?, '2026-08-19', ?, UUID_TO_BIN(?)
                )
                """,
                UUID.randomUUID().toString(),
                "c1000000-0000-0000-0000-000000000003",
                new BigDecimal("120000.00"),
                Instant.parse("2026-08-19T00:00:00Z"),
                ADMIN_ID.toString()
        );
        now.set(Instant.parse("2026-08-20T08:00:00Z"));

        var invoice = createInvoiceService.create(new CreateInvoiceCommand(fixture.visitId(), null));

        assertEquals(new BigDecimal("195000.00"), invoice.totalAmount());
        var serviceLine = invoice.lines().stream()
                .filter(line -> line.lineType() == InvoiceLineType.SERVICE_FEE)
                .findFirst()
                .orElseThrow();
        assertEquals(fixture.itemId(), serviceLine.referenceId());
        assertEquals(new BigDecimal("95000.00"), serviceLine.amount());
    }

    private Fixture seedCompletedClinicalOrder() {
        UUID visitId = UUID.randomUUID();
        UUID recordId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID patientId = UUID.fromString(jdbc.queryForObject(
                "SELECT BIN_TO_UUID(id) FROM patients ORDER BY created_at LIMIT 1", String.class));
        UUID doctorId = UUID.fromString(jdbc.queryForObject(
                "SELECT BIN_TO_UUID(id) FROM users WHERE username = 'doctor1'", String.class));
        Instant createdAt = Instant.parse("2026-08-18T07:00:00Z");

        jdbc.update("""
                INSERT INTO visits (
                    id, visit_code, patient_id, doctor_id, visit_type, status, visit_at,
                    started_at, completed_at, reason, created_by, created_at
                ) VALUES (
                    UUID_TO_BIN(?), ?, UUID_TO_BIN(?), UUID_TO_BIN(?), 'WALK_IN', 'COMPLETED', ?,
                    ?, ?, 'Clinical billing E2E', UUID_TO_BIN(?), ?
                )
                """,
                visitId.toString(), "VIS-E2E-" + visitId.toString().substring(0, 8),
                patientId.toString(), doctorId.toString(), createdAt, createdAt, createdAt,
                ADMIN_ID.toString(), createdAt
        );
        jdbc.update("""
                INSERT INTO medical_records (id, visit_id, status, created_by, created_at)
                VALUES (UUID_TO_BIN(?), UUID_TO_BIN(?), 'OPEN', UUID_TO_BIN(?), ?)
                """,
                recordId.toString(), visitId.toString(), ADMIN_ID.toString(), createdAt
        );
        jdbc.update("""
                INSERT INTO clinical_orders (
                    id, order_code, visit_id, medical_record_id, patient_id, ordered_by,
                    status, ordered_at, completed_at, created_at
                ) VALUES (
                    UUID_TO_BIN(?), ?, UUID_TO_BIN(?), UUID_TO_BIN(?), UUID_TO_BIN(?), UUID_TO_BIN(?),
                    'COMPLETED', ?, ?, ?
                )
                """,
                orderId.toString(), "ORD-E2E-" + orderId.toString().substring(0, 8),
                visitId.toString(), recordId.toString(), patientId.toString(), doctorId.toString(),
                createdAt, createdAt, createdAt
        );
        jdbc.update("""
                INSERT INTO clinical_order_items (
                    id, clinical_order_id, clinical_service_id, service_code, service_name,
                    status, created_at
                ) VALUES (
                    UUID_TO_BIN(?), UUID_TO_BIN(?), UUID_TO_BIN(?), 'LAB-CBC', 'CBC snapshot name',
                    'COMPLETED', ?
                )
                """,
                itemId.toString(), orderId.toString(), CLINICAL_SERVICE_ID.toString(), createdAt
        );
        return new Fixture(visitId, itemId);
    }

    private record Fixture(UUID visitId, UUID itemId) {
    }
}
