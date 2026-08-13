package com.benhsoan.application.ucservice.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.result.OperationalSummaryResult;
import com.benhsoan.port.dto.result.OperationalTimelineItemResult;
import com.benhsoan.port.dto.result.OperationalTimelineResult;
import com.benhsoan.port.dto.result.TopMedicineItemResult;
import com.benhsoan.port.dto.result.TopMedicinesReportResult;
import com.benhsoan.port.outbound.repository.reporting.DailyRevenueSummary;
import com.benhsoan.port.outbound.repository.reporting.DailyVisitSummary;
import com.benhsoan.port.outbound.repository.reporting.OperationalReportQueryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperationalReportDataService {

    private static final String DEFAULT_CURRENCY = "VND";

    private final OperationalReportQueryRepository operationalReportQueryRepository;

    public OperationalSummaryResult getSummary(LocalDate from, LocalDate to) {
        ReportingTimeRange range = ReportingTimeRange.of(from, to);

        return new OperationalSummaryResult(
                from,
                to,
                operationalReportQueryRepository.countCompletedVisits(range.fromInclusive(), range.toExclusive()),
                operationalReportQueryRepository.sumNetRevenue(range.fromInclusive(), range.toExclusive()),
                DEFAULT_CURRENCY
        );
    }

    public OperationalTimelineResult getTimeline(LocalDate from, LocalDate to) {
        ReportingTimeRange range = ReportingTimeRange.of(from, to);
        Map<LocalDate, Long> visitsByDate = aggregateVisitsByDate(
                operationalReportQueryRepository.findDailyCompletedVisits(range.fromInclusive(), range.toExclusive()));
        Map<LocalDate, BigDecimal> revenueByDate = aggregateRevenueByDate(
                operationalReportQueryRepository.findDailyNetRevenue(range.fromInclusive(), range.toExclusive()));

        return new OperationalTimelineResult(from, to, buildItems(from, to, visitsByDate, revenueByDate));
    }

    public OperationalReportData getReportData(LocalDate from, LocalDate to) {
        return new OperationalReportData(
                getSummary(from, to),
                getTimeline(from, to)
        );
    }

    public TopMedicinesReportResult getTopMedicines(LocalDate from, LocalDate to) {
        ReportingTimeRange range = ReportingTimeRange.of(from, to);

        return new TopMedicinesReportResult(
                from,
                to,
                operationalReportQueryRepository.findTopDispensedMedicines(range.fromInclusive(), range.toExclusive())
                        .stream()
                        .map(item -> new TopMedicineItemResult(
                                item.medicineId(),
                                item.medicineCode(),
                                item.medicineName(),
                                item.totalDispensedQuantity()))
                        .toList()
        );
    }

    private Map<LocalDate, Long> aggregateVisitsByDate(List<DailyVisitSummary> visits) {
        Map<LocalDate, Long> result = new HashMap<>();
        for (DailyVisitSummary visit : visits) {
            result.put(visit.date(), visit.visitCount());
        }
        return result;
    }

    private Map<LocalDate, BigDecimal> aggregateRevenueByDate(List<DailyRevenueSummary> revenues) {
        Map<LocalDate, BigDecimal> result = new HashMap<>();
        for (DailyRevenueSummary revenue : revenues) {
            result.put(revenue.date(), revenue.revenue());
        }
        return result;
    }

    private List<OperationalTimelineItemResult> buildItems(
            LocalDate from,
            LocalDate to,
            Map<LocalDate, Long> visitsByDate,
            Map<LocalDate, BigDecimal> revenueByDate
    ) {
        java.util.ArrayList<OperationalTimelineItemResult> items = new java.util.ArrayList<>();
        LocalDate current = from;
        while (!current.isAfter(to)) {
            items.add(new OperationalTimelineItemResult(
                    current,
                    visitsByDate.getOrDefault(current, 0L),
                    revenueByDate.getOrDefault(current, BigDecimal.ZERO)
            ));
            current = current.plusDays(1);
        }
        return items;
    }
}
