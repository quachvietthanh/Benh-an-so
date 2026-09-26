package com.benhsoan.port.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PayableEncounterResult(
        UUID visitId,
        String visitCode,
        UUID patientId,
        String patientCode,
        String patientName,
        String reason,
        Instant completedAt,
        BigDecimal examFee,
        BigDecimal medicineFee,
        BigDecimal serviceFee,
        BigDecimal totalEstimatedAmount,
        boolean hasPrescription,
        boolean hasPendingDispense
) {
    public PayableEncounterResult(
            UUID visitId,
            String visitCode,
            UUID patientId,
            String patientCode,
            String patientName,
            String reason,
            Instant completedAt,
            BigDecimal examFee,
            BigDecimal medicineFee,
            BigDecimal serviceFee,
            BigDecimal totalEstimatedAmount
    ) {
        this(visitId, visitCode, patientId, patientCode, patientName, reason, completedAt,
                examFee, medicineFee, serviceFee, totalEstimatedAmount, false, false);
    }

    public PayableEncounterResult(
            UUID visitId,
            String visitCode,
            UUID patientId,
            String patientCode,
            String patientName,
            String reason,
            Instant completedAt
    ) {
        this(visitId, visitCode, patientId, patientCode, patientName, reason, completedAt,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, false);
    }
}
