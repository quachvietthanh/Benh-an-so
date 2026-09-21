package com.benhsoan.application.ucservice.reporting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.result.AppointmentEffectivenessReportResult;
import com.benhsoan.port.dto.result.AppointmentStatusCountResult;
import com.benhsoan.port.inbound.reporting.GetAppointmentEffectivenessReportUseCase;
import com.benhsoan.port.outbound.repository.reporting.AppointmentEffectivenessQueryRepository;
import com.benhsoan.port.outbound.repository.reporting.AppointmentStatusCountSummary;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAppointmentEffectivenessReportService implements GetAppointmentEffectivenessReportUseCase {

    private static final String MANAGER_ROLE = "MANAGER";
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final AppointmentEffectivenessQueryRepository queryRepository;
    private final ClockPort clockPort;
    private final CurrentUserPort currentUserPort;

    @Override
    public AppointmentEffectivenessReportResult getReport(
            LocalDate from,
            LocalDate to,
            UUID doctorId,
            String bookingChannel
    ) {
        ensureAuthorized();

        ReportingTimeRange range = ReportingTimeRange.of(from, to);

        List<AppointmentStatusCountSummary> counts = queryRepository.findStatusCounts(
                range.fromInclusive(), range.toExclusive(), doctorId, bookingChannel);

        long total = counts.stream()
                .mapToLong(AppointmentStatusCountSummary::count)
                .sum();

        List<AppointmentStatusCountResult> items = counts.stream()
                .map(summary -> toItem(summary, total))
                .sorted(Comparator
                        .comparing(AppointmentStatusCountResult::bookingChannel,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparingInt(item -> item.status().ordinal()))
                .toList();

        return new AppointmentEffectivenessReportResult(from, to, clockPort.now(), total, items);
    }

    private AppointmentStatusCountResult toItem(AppointmentStatusCountSummary summary, long total) {
        BigDecimal percentage = total == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(summary.count())
                        .multiply(ONE_HUNDRED)
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        return new AppointmentStatusCountResult(
                summary.bookingChannel(), summary.status(), summary.count(), percentage);
    }

    private void ensureAuthorized() {
        if (!currentUserPort.hasRole(MANAGER_ROLE)) {
            throw new AccessDeniedException(
                    "Only managers can view the appointment effectiveness report.");
        }
    }
}
