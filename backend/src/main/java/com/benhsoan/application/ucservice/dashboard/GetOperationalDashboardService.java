package com.benhsoan.application.ucservice.dashboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.inventory.EligibleStockSnapshotService;
import com.benhsoan.application.ucservice.inventory.LowStockEvaluator;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.dto.result.OperationalDashboardResult;
import com.benhsoan.port.inbound.dashboard.GetOperationalDashboardUseCase;
import com.benhsoan.port.outbound.repository.billing.PaymentRepository;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetOperationalDashboardService implements GetOperationalDashboardUseCase {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String MANAGER_ROLE = "MANAGER";
    private static final String CLINIC_MANAGER_ROLE = "CLINIC_MANAGER";
    private static final List<PaymentStatus> REVENUE_STATUSES =
            List.of(PaymentStatus.RECORDED, PaymentStatus.SUCCESS);

    private final VisitRepository visitRepository;
    private final PaymentRepository paymentRepository;
    private final MedicineRepository medicineRepository;
    private final MedicineBatchRepository medicineBatchRepository;
    private final EligibleStockSnapshotService eligibleStockSnapshotService;
    private final LowStockEvaluator lowStockEvaluator;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Value("${inventory.expiry-alert-days:30}")
    private long expiryAlertDays;

    @Override
    public OperationalDashboardResult get() {
        ensureAuthorized();

        Instant now = clockPort.now();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        Instant startOfDay = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant startOfNextDay = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        OperationalDashboardResult.VisitSummary visitSummary = summarizeVisits(
                visitRepository.findByVisitAtBetween(startOfDay, startOfNextDay)
        );

        BigDecimal totalRevenueToday = paymentRepository
                .sumAmountPaidByStatusInAndPaidAtBetween(REVENUE_STATUSES, startOfDay, startOfNextDay);

        int lowStockCount = countLowStockMedicines(today);
        int expiryAlertCount = countExpiryAlertBatches(today);

        return new OperationalDashboardResult(
                visitSummary,
                new OperationalDashboardResult.RevenueSummary(totalRevenueToday),
                new OperationalDashboardResult.InventoryAlertSummary(lowStockCount, expiryAlertCount),
                now
        );
    }

    private void ensureAuthorized() {
        if (!currentUserPort.hasRole(ADMIN_ROLE)
                && !currentUserPort.hasRole(MANAGER_ROLE)
                && !currentUserPort.hasRole(CLINIC_MANAGER_ROLE)) {
            throw new AccessDeniedException(
                    "Only managers or admins can view the operational dashboard."
            );
        }
    }

    private OperationalDashboardResult.VisitSummary summarizeVisits(List<Visit> visits) {
        int waiting = 0;
        int inProgress = 0;
        int completed = 0;
        int cancelled = 0;

        for (Visit visit : visits) {
            switch (visit.getStatus()) {
                case WAITING -> waiting++;
                case IN_PROGRESS, WAITING_FOR_RESULT -> inProgress++;
                case COMPLETED -> completed++;
                case CANCELLED -> cancelled++;
            }
        }

        return new OperationalDashboardResult.VisitSummary(
                visits.size(),
                waiting,
                inProgress,
                completed,
                cancelled
        );
    }

    private int countLowStockMedicines(LocalDate today) {
        List<Medicine> medicines = medicineRepository.findAllActive();
        Map<UUID, Integer> eligibleStockByMedicineId = eligibleStockSnapshotService
                .snapshotEligibleStockQuantities(
                        medicines.stream().map(Medicine::getId).toList(),
                        today
                );

        return (int) medicines.stream()
                .filter(medicine -> lowStockEvaluator.isLowStockByThreshold(
                        eligibleStockByMedicineId.getOrDefault(medicine.getId(), 0),
                        medicine.getMinStockThreshold()
                ))
                .count();
    }

    private int countExpiryAlertBatches(LocalDate today) {
        return (int) medicineBatchRepository.findAll().stream()
                .filter(batch -> batch.isExpiredOn(today) || batch.isNearExpiryOn(today, expiryAlertDays))
                .count();
    }
}
