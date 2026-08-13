package com.benhsoan.application.ucservice.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.dto.result.OperationalSummaryResult;
import com.benhsoan.port.inbound.reporting.GetOperationalSummaryUseCase;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetOperationalSummaryService implements GetOperationalSummaryUseCase {

    private final VisitRepository visitRepository;
    private final InvoiceRepository invoiceRepository;

    @Override
    public OperationalSummaryResult getSummary(LocalDate from, LocalDate to) {
        ReportingTimeRange range = ReportingTimeRange.of(from, to);
        List<Visit> visits = visitRepository.findCompletedBetween(range.fromInclusive(), range.toExclusive());
        List<Invoice> invoices = invoiceRepository.findCreatedBetween(range.fromInclusive(), range.toExclusive());

        return new OperationalSummaryResult(
                from,
                to,
                visits.size(),
                calculateNetRevenue(invoices),
                "VND"
        );
    }

    private BigDecimal calculateNetRevenue(List<Invoice> invoices) {
        return invoices.stream()
                .map(invoice -> invoice.getType() == InvoiceType.ADJUSTMENT
                        ? invoice.getTotalAmount().negate()
                        : invoice.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
