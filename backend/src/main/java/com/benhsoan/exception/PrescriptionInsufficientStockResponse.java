package com.benhsoan.exception;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.prescription.exception.PrescriptionInsufficientStockException.StockShortageDetail;

public record PrescriptionInsufficientStockResponse(
        Instant timestamp,
        String code,
        String message,
        UUID prescriptionId,
        List<StockShortageDetail> details
) {
}
