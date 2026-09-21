package com.benhsoan.application.ucservice.reporting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.result.AppointmentEffectivenessReportResult;
import com.benhsoan.port.dto.result.AppointmentStatusCountResult;
import com.benhsoan.port.inbound.reporting.GetAppointmentEffectivenessReportUseCase;
import com.benhsoan.port.outbound.repository.reporting.AppointmentEffectivenessQueryRepository;
import com.benhsoan.port.outbound.repository.reporting.AppointmentStatusCountSummary;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAppointmentEffectivenessReportService implements GetAppointmentEffectivenessReportUseCase {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final AppointmentEffectivenessQueryRepository queryRepository;
    private final ClockPort clockPort;

    @Override
    public AppointmentEffectivenessReportResult getReport(
            LocalDate from,
            LocalDate to,
            UUID doctorId,
            String bookingChannel
    ) {
        ReportingTimeRange range = ReportingTimeRange.of(from, to);

        List<AppointmentStatusCountSummary> counts = queryRepository.findStatusCounts(
                range.fromInclusive(), range.toExclusive(), doctorId, bookingChannel);

        long total = counts.stream()
                .mapToLong(AppointmentStatusCountSummary::count)
                .sum();

        List<AppointmentStatusCountResult> items = counts.stream()
                .map(summary -> toItem(summary, total))
                .sorted(Comparator.comparingInt(item -> item.status().ordinal()))
                .toList();

        return new AppointmentEffectivenessReportResult(from, to, clockPort.now(), total, items);
    }

    private AppointmentStatusCountResult toItem(AppointmentStatusCountSummary summary, long total) {
        BigDecimal percentage = total == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(summary.count())
                        .multiply(ONE_HUNDRED)
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        return new AppointmentStatusCountResult(summary.status(), summary.count(), percentage);
    }
}
