package com.benhsoan.port.outbound.repository.billing;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.InvoiceType;

public record InvoiceSearchCriteria(
        String invoiceCode,
        InvoiceType invoiceType,
        UUID visitId,
        String patientName,
        Instant createdFrom,
        Instant createdTo
) {
}
