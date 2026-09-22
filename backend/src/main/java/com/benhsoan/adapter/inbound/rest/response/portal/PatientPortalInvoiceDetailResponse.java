package com.benhsoan.adapter.inbound.rest.response.portal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PatientPortalInvoiceDetailResponse(
        UUID invoiceId,
        String invoiceCode,
        String invoiceType,
        UUID originalInvoiceId,
        String originalInvoiceCode,
        String adjustmentReason,
        BigDecimal totalAmount,
        Instant createdAt,
        String creatorName,
        UUID visitId,
        String visitCode,
        Instant visitDate,
        String doctorName,
        String specialtyName,
        List<InvoiceLineItemResponse> items
) {

    public record InvoiceLineItemResponse(
            UUID lineId,
            String lineType,
            String itemName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal amount
    ) {
    }
}
