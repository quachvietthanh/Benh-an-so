package com.benhsoan.application.ucservice.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.dto.result.OperationalTimelineItemResult;
import com.benhsoan.port.dto.result.OperationalTimelineResult;
import com.benhsoan.port.inbound.reporting.GetOperationalTimelineUseCase;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetOperationalTimelineService implements GetOperationalTimelineUseCase {

    private final VisitRepository visitRepository;
    private final InvoiceRepository invoiceRepository;

    @Override
    public OperationalTimelineResult getTimeline(LocalDate from, LocalDate to) {
        ReportingTimeRange range = ReportingTimeRange.of(from, to);
        List<Visit> visits = visitRepository.findCompletedBetween(range.fromInclusive(), range.toExclusive());
        List<Invoice> invoices = invoiceRepository.findCreatedBetween(range.fromInclusive(), range.toExclusive());

        Map<LocalDate, Long> visitsByDate = aggregateVisitsByDate(visits);
        Map<LocalDate, BigDecimal> revenueByDate = aggregateRevenueByDate(invoices);

        return new OperationalTimelineResult(from, to, buildItems(from, to, visitsByDate, revenueByDate));
    }

    private Map<LocalDate, Long> aggregateVisitsByDate(List<Visit> visits) {
        Map<LocalDate, Long> result = new HashMap<>();
        for (Visit visit : visits) {
            LocalDate date = visit.getCompletedAt().atZone(ZoneOffset.UTC).toLocalDate();
            result.merge(date, 1L, Long::sum);
        }
        return result;
    }

    private Map<LocalDate, BigDecimal> aggregateRevenueByDate(List<Invoice> invoices) {
        Map<LocalDate, BigDecimal> result = new HashMap<>();
        for (Invoice invoice : invoices) {
            LocalDate date = invoice.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
            BigDecimal signedAmount = invoice.getType() == InvoiceType.ADJUSTMENT
                    ? invoice.getTotalAmount().negate()
                    : invoice.getTotalAmount();
            result.merge(date, signedAmount, BigDecimal::add);
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
