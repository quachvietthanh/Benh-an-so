package com.benhsoan.application.ucservice.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.benhsoan.application.ucservice.inventory.EligibleStockSnapshotService;
import com.benhsoan.application.ucservice.inventory.LowStockEvaluator;
import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.medicine.enums.DosageForm;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.result.OperationalDashboardResult;
import com.benhsoan.port.outbound.repository.billing.PaymentRepository;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class GetOperationalDashboardServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-11T08:00:00Z");
    private static final LocalDate TODAY = LocalDate.ofInstant(NOW, ZoneOffset.UTC);
    private static final Instant START_OF_DAY = TODAY.atStartOfDay(ZoneOffset.UTC).toInstant();
    private static final Instant START_OF_NEXT_DAY =
            TODAY.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

    private final VisitRepository visitRepository = mock(VisitRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final MedicineRepository medicineRepository = mock(MedicineRepository.class);
    private final MedicineBatchRepository medicineBatchRepository = mock(MedicineBatchRepository.class);
    private final EligibleStockSnapshotService eligibleStockSnapshotService =
            mock(EligibleStockSnapshotService.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private GetOperationalDashboardService service;

    @BeforeEach
    void setUp() throws Exception {
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(currentUserPort.hasRole("CLINIC_MANAGER")).thenReturn(false);
        when(clockPort.now()).thenReturn(NOW);
        when(paymentRepository.sumRefundedAmountByRefundedAtBetween(
                START_OF_DAY,
                START_OF_NEXT_DAY
        )).thenReturn(BigDecimal.ZERO);

        service = new GetOperationalDashboardService(
                visitRepository,
                paymentRepository,
                medicineRepository,
                medicineBatchRepository,
                eligibleStockSnapshotService,
                new LowStockEvaluator(),
                currentUserPort,
                clockPort
        );

        Field field = GetOperationalDashboardService.class.getDeclaredField("expiryAlertDays");
        field.setAccessible(true);
        field.set(service, 30L);
    }

    @Test
    void aggregatesTodaysMetrics() {
        UUID med1 = UUID.randomUUID();
        UUID med2 = UUID.randomUUID();

        when(visitRepository.findByVisitAtBetween(START_OF_DAY, START_OF_NEXT_DAY))
                .thenReturn(List.of(
                        visit(VisitStatus.WAITING),
                        visit(VisitStatus.WAITING),
                        visit(VisitStatus.IN_PROGRESS),
                        visit(VisitStatus.WAITING_FOR_RESULT),
                        visit(VisitStatus.COMPLETED),
                        visit(VisitStatus.COMPLETED),
                        visit(VisitStatus.CANCELLED)
                ));

        when(paymentRepository.sumAmountPaidByStatusInAndPaidAtBetween(
                any(), eq(START_OF_DAY), eq(START_OF_NEXT_DAY)))
                .thenReturn(new BigDecimal("1250.50"));

        when(medicineRepository.findAllActive()).thenReturn(List.of(
                medicine(med1, "MED-001", 20),
                medicine(med2, "MED-002", 30)
        ));

        when(eligibleStockSnapshotService.snapshotEligibleStockQuantities(any(), any()))
                .thenReturn(Map.of(med1, 10, med2, 30));

        when(medicineBatchRepository.findAll()).thenReturn(List.of(
                batch(med1, "B-EXPIRED", LocalDate.of(2026, 8, 10), 5),
                batch(med1, "B-NEAR", LocalDate.of(2026, 8, 30), 8),
                batch(med2, "B-FAR", LocalDate.of(2026, 12, 1), 20)
        ));

        OperationalDashboardResult result = service.get();

        assertEquals(7, result.visitSummary().total());
        assertEquals(2, result.visitSummary().waiting());
        assertEquals(2, result.visitSummary().inProgress());
        assertEquals(2, result.visitSummary().completed());
        assertEquals(1, result.visitSummary().cancelled());

        assertEquals(new BigDecimal("1250.50"), result.revenueSummary().totalRevenueToday());

        assertEquals(1, result.inventoryAlertSummary().lowStockCount());
        assertEquals(2, result.inventoryAlertSummary().expiryAlertCount());

        assertEquals(NOW, result.asOf());
    }
    @Test
    void returnsZeroesForEmptyDay() {
        when(visitRepository.findByVisitAtBetween(START_OF_DAY, START_OF_NEXT_DAY))
                .thenReturn(List.of());
        when(paymentRepository.sumAmountPaidByStatusInAndPaidAtBetween(
                any(), eq(START_OF_DAY), eq(START_OF_NEXT_DAY)))
                .thenReturn(BigDecimal.ZERO);
        when(medicineRepository.findAllActive()).thenReturn(List.of());
        when(eligibleStockSnapshotService.snapshotEligibleStockQuantities(any(), any()))
                .thenReturn(Map.of());
        when(medicineBatchRepository.findAll()).thenReturn(List.of());

        OperationalDashboardResult result = service.get();

        assertEquals(0, result.visitSummary().total());
        assertEquals(0, result.visitSummary().waiting());
        assertEquals(0, result.visitSummary().inProgress());
        assertEquals(0, result.visitSummary().completed());
        assertEquals(0, result.visitSummary().cancelled());
        assertEquals(BigDecimal.ZERO, result.revenueSummary().totalRevenueToday());
        assertEquals(0, result.inventoryAlertSummary().lowStockCount());
        assertEquals(0, result.inventoryAlertSummary().expiryAlertCount());
        assertEquals(NOW, result.asOf());
    }

    @Test
    void subtractsRefundsInRefundPeriodWithoutRemovingOriginalCollections() {
        when(visitRepository.findByVisitAtBetween(START_OF_DAY, START_OF_NEXT_DAY))
                .thenReturn(List.of());
        when(paymentRepository.sumAmountPaidByStatusInAndPaidAtBetween(
                List.of(
                        PaymentStatus.RECORDED,
                        PaymentStatus.SUCCESS,
                        PaymentStatus.REFUNDED
                ),
                START_OF_DAY,
                START_OF_NEXT_DAY
        )).thenReturn(new BigDecimal("1000000"));
        when(paymentRepository.sumRefundedAmountByRefundedAtBetween(
                START_OF_DAY,
                START_OF_NEXT_DAY
        )).thenReturn(new BigDecimal("250000"));
        when(medicineRepository.findAllActive()).thenReturn(List.of());
        when(eligibleStockSnapshotService.snapshotEligibleStockQuantities(any(), any()))
                .thenReturn(Map.of());
        when(medicineBatchRepository.findAll()).thenReturn(List.of());

        OperationalDashboardResult result = service.get();

        assertEquals(
                new BigDecimal("750000"),
                result.revenueSummary().totalRevenueToday()
        );
    }

    private Visit visit(VisitStatus status) {
        return Visit.restore(
                UUID.randomUUID(),
                "VISIT-" + UUID.randomUUID().toString().substring(0, 8),
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                VisitType.WALK_IN,
                status,
                NOW,
                status == VisitStatus.IN_PROGRESS ? NOW : null,
                status == VisitStatus.COMPLETED ? NOW : null,
                "Checkup",
                null,
                UUID.randomUUID(),
                NOW,
                null
        );
    }

    private Medicine medicine(UUID id, String code, int minStockThreshold) {
        return Medicine.restore(
                id,
                code,
                "Medicine " + code,
                "ingredient",
                "500 mg",
                DosageForm.TABLET,
                "vien",
                AdministrationRoute.ORAL,
                true,
                NOW.minusSeconds(7200),
                null,
                100,
                minStockThreshold
        );
    }

    private MedicineBatch batch(UUID medicineId, String batchNumber, LocalDate expiryDate, int quantity) {
        return MedicineBatch.restore(
                UUID.randomUUID(),
                medicineId,
                batchNumber,
                expiryDate,
                quantity,
                BatchStatus.ACTIVE,
                NOW.minusSeconds(3600),
                null
        );
    }
}
