package com.benhsoan.application.ucservice.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.command.billing.PayableEncounterQuery;
import com.benhsoan.port.dto.result.PayableEncounterResult;
import com.benhsoan.port.inbound.billing.GetPayableEncountersUseCase;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.repository.billing.PayableEncounterSummary;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPayableEncountersService implements GetPayableEncountersUseCase {

    private static final ZoneId BILLING_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final BigDecimal DEFAULT_EXAM_FEE = new BigDecimal("100000");
    private static final BigDecimal DEFAULT_MEDICINE_FEE = BigDecimal.ZERO;

    private final InvoiceRepository invoiceRepository;
    private final PayableEncounterResultMapper resultMapper;
    private final ClinicalServiceFeeCalculator clinicalServiceFeeCalculator;
    private final ClockPort clockPort;

    @Override
    public Page<PayableEncounterResult> get(PayableEncounterQuery query) {
        PayableEncounterQuery safeQuery = query != null ? query : new PayableEncounterQuery(null, null, Pageable.unpaged());
        LocalDate date = safeQuery.date();
        String search = (safeQuery.search() != null && !safeQuery.search().isBlank()) ? safeQuery.search().trim() : null;
        Pageable pageable = safeQuery.pageable() != null ? safeQuery.pageable() : Pageable.unpaged();

        Instant fromCompletedAt = date != null ? date.atStartOfDay(BILLING_ZONE).toInstant() : null;
        Instant toCompletedAt = date != null ? date.plusDays(1).atStartOfDay(BILLING_ZONE).toInstant() : null;

        Instant now = clockPort.now();
        Page<PayableEncounterSummary> page = invoiceRepository.findPayableEncounters(fromCompletedAt, toCompletedAt, search, pageable);

        List<UUID> visitIds = page.getContent().stream()
                .map(PayableEncounterSummary::visitId)
                .toList();
        Map<UUID, BigDecimal> serviceFees = clinicalServiceFeeCalculator.calculateBatch(visitIds, now);

        return page.map(summary -> {
            BigDecimal serviceFee = serviceFees.getOrDefault(summary.visitId(), BigDecimal.ZERO);
            BigDecimal examFee = DEFAULT_EXAM_FEE;
            BigDecimal medicineFee = DEFAULT_MEDICINE_FEE;
            BigDecimal totalEstimatedAmount = examFee.add(medicineFee).add(serviceFee);
            return resultMapper.toResult(
                    summary,
                    examFee,
                    medicineFee,
                    serviceFee,
                    totalEstimatedAmount,
                    summary.hasPrescription(),
                    summary.hasPendingDispense()
            );
        });
    }
}
