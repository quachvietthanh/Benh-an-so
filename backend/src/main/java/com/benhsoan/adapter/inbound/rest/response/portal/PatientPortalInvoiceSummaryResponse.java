package com.benhsoan.adapter.inbound.rest.response.portal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PatientPortalInvoiceSummaryResponse(
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
