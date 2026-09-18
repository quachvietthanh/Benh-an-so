package com.benhsoan.port.outbound.repository.reporting;

import java.math.BigDecimal;
import java.util.UUID;

import com.benhsoan.domain.billing.enums.InvoiceLineType;
import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;

public record InvoiceLineReportDetail(
        UUID lineId,
        UUID invoiceId,
        InvoiceType invoiceType,
        InvoiceLineType lineType,
        InvoiceLineType targetLineType,
        String itemName,
        BigDecimal amount,
        UUID referenceId,
        UUID visitId,
        UUID doctorId,
        String doctorCode,
        String doctorName,
        ClinicalServiceType clinicalServiceType
) {
    public InvoiceLineReportDetail(
            UUID lineId,
            UUID invoiceId,
            InvoiceType invoiceType,
            InvoiceLineType lineType,
            String itemName,
            BigDecimal amount,
            UUID referenceId,
            UUID visitId,
            UUID doctorId,
            String doctorCode,
            String doctorName,
            ClinicalServiceType clinicalServiceType
    ) {
        this(
                lineId,
                invoiceId,
                invoiceType,
                lineType,
                lineType,
                itemName,
                amount,
                referenceId,
                visitId,
                doctorId,
                doctorCode,
                doctorName,
                clinicalServiceType
        );
    }
}
