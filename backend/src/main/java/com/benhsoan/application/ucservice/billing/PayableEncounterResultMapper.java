package com.benhsoan.application.ucservice.billing;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.benhsoan.port.dto.result.PayableEncounterResult;
import com.benhsoan.port.outbound.repository.billing.PayableEncounterSummary;

@Component
public class PayableEncounterResultMapper {

    public PayableEncounterResult toResult(
            PayableEncounterSummary summary,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal serviceFee,
            BigDecimal totalEstimatedAmount,
            boolean hasPrescription,
            boolean hasPendingDispense
    ) {
        return new PayableEncounterResult(
                summary.visitId(),
                summary.visitCode(),
                summary.patientId(),
                summary.patientCode(),
                summary.patientName(),
                summary.reason(),
                summary.completedAt(),
                examFee,
                medicineFee,
                serviceFee,
                totalEstimatedAmount,
                hasPrescription,
                hasPendingDispense
        );
    }

    public PayableEncounterResult toResult(
            PayableEncounterSummary summary,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal serviceFee,
            BigDecimal totalEstimatedAmount
    ) {
        return toResult(summary, examFee, medicineFee, serviceFee, totalEstimatedAmount,
                summary.hasPrescription(), summary.hasPendingDispense());
    }

    public PayableEncounterResult toResult(PayableEncounterSummary summary) {
        return toResult(summary, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
