package com.benhsoan.port.dto.result.portal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PatientPortalInvoiceSummaryResult(
        UUID invoiceId,
        String invoiceCode,
        String invoiceType,
        BigDecimal totalAmount,
        Instant createdAt,
        UUID visitId,
        String visitCode,
        Instant visitDate,
        String doctorName,
        String specialtyName,
        int itemCount
) {
}
