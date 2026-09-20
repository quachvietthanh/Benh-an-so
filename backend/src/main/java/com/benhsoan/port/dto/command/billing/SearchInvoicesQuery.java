package com.benhsoan.port.dto.command.billing;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.billing.enums.InvoiceType;

public record SearchInvoicesQuery(
        String invoiceCode,
        InvoiceType invoiceType,
        UUID visitId,
        String patientName,
        Instant createdFrom,
        Instant createdTo,
        Pageable pageable
) {
}
