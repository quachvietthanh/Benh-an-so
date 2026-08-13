package com.benhsoan.persistence.adapterRepository.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.persistence.entity.billing.InvoiceEntity;
import com.benhsoan.persistence.entity.visit.VisitEntity;
import com.benhsoan.persistence.jpaRepository.billing.JpaInvoiceRepository;
import com.benhsoan.persistence.jpaRepository.visit.JpaVisitRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:reporting-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class OperationalReportQueryRepositoryAdapterIntegrationTest {

    @Autowired private JpaInvoiceRepository invoiceRepository;
    @Autowired private JpaVisitRepository visitRepository;
    @Autowired private EntityManager entityManager;

    private OperationalReportQueryRepositoryAdapter repositoryAdapter;

    @BeforeEach
    void setUp() {
        repositoryAdapter = new OperationalReportQueryRepositoryAdapter(entityManager);
    }

    @Test
    void countsCompletedVisitsInsideRangeOnly() {
        createVisit("VIS000001", VisitStatus.COMPLETED, Instant.parse("2026-08-01T02:00:00Z"));
        createVisit("VIS000002", VisitStatus.COMPLETED, Instant.parse("2026-08-03T04:00:00Z"));
        createVisit("VIS000003", VisitStatus.IN_PROGRESS, null);
        createVisit("VIS000004", VisitStatus.COMPLETED, Instant.parse("2026-08-04T00:00:00Z"));

        long count = repositoryAdapter.countCompletedVisits(
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-04T00:00:00Z")
        );

        assertEquals(2L, count);
    }

    @Test
    void sumsNetRevenueUsingOriginalInvoicesAndNegativeAdjustments() {
        UUID visitId = UUID.randomUUID();
        createOriginalInvoice("HD000001", visitId, new BigDecimal("100000"), Instant.parse("2026-08-01T01:00:00Z"));
        createOriginalInvoice("HD000002", visitId, new BigDecimal("50000"), Instant.parse("2026-08-02T01:00:00Z"));
        createAdjustmentInvoice("HDDC000001", visitId, new BigDecimal("-20000"), Instant.parse("2026-08-03T01:00:00Z"));
        createAdjustmentInvoice("HDDC000002", visitId, new BigDecimal("-5000"), Instant.parse("2026-08-05T01:00:00Z"));

        BigDecimal revenue = repositoryAdapter.sumNetRevenue(
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-04T00:00:00Z")
        );

        assertEquals(new BigDecimal("130000.00"), revenue);

        var timeline = repositoryAdapter.findDailyNetRevenue(
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-04T00:00:00Z")
        );

        assertEquals(3, timeline.size());
        assertEquals(LocalDate.of(2026, 8, 1), timeline.get(0).date());
        assertEquals(new BigDecimal("100000.00"), timeline.get(0).revenue());
        assertEquals(LocalDate.of(2026, 8, 2), timeline.get(1).date());
        assertEquals(new BigDecimal("50000.00"), timeline.get(1).revenue());
        assertEquals(LocalDate.of(2026, 8, 3), timeline.get(2).date());
        assertEquals(new BigDecimal("-20000.00"), timeline.get(2).revenue());
    }

    private void createVisit(String code, VisitStatus status, Instant completedAt) {
        visitRepository.save(VisitEntity.builder()
                .id(UUID.randomUUID())
                .visitCode(code)
                .patientId(UUID.randomUUID())
                .doctorId(UUID.randomUUID())
                .visitType(VisitType.WALK_IN)
                .status(status)
                .visitAt(Instant.parse("2026-08-01T00:00:00Z"))
                .startedAt(Instant.parse("2026-08-01T00:30:00Z"))
                .completedAt(completedAt)
                .reason("Kham tong quat")
                .createdBy(UUID.randomUUID())
                .createdAt(Instant.parse("2026-08-01T00:00:00Z"))
                .build());
    }

    private void createOriginalInvoice(String code, UUID visitId, BigDecimal amount, Instant createdAt) {
        invoiceRepository.save(InvoiceEntity.builder()
                .id(UUID.randomUUID())
                .invoiceCode(code)
                .visitId(visitId)
                .paymentId(UUID.randomUUID())
                .type(InvoiceType.ORIGINAL)
                .totalAmount(amount)
                .createdBy(UUID.randomUUID())
                .createdAt(createdAt)
                .build());
    }

    private void createAdjustmentInvoice(String code, UUID visitId, BigDecimal amount, Instant createdAt) {
        invoiceRepository.save(InvoiceEntity.builder()
                .id(UUID.randomUUID())
                .invoiceCode(code)
                .visitId(visitId)
                .type(InvoiceType.ADJUSTMENT)
                .originalInvoiceId(UUID.randomUUID())
                .adjustmentReason("Dieu chinh")
                .totalAmount(amount)
                .createdBy(UUID.randomUUID())
                .createdAt(createdAt)
                .build());
    }
}
