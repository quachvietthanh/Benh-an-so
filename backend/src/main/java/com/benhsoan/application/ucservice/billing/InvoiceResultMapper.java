package com.benhsoan.application.ucservice.billing;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.port.dto.result.InvoiceLineResult;
import com.benhsoan.port.dto.result.InvoiceResult;

@Component
public class InvoiceResultMapper {

    public InvoiceResult toResult(Invoice invoice) {
        List<InvoiceLineResult> lines = invoice.getLines()
                .stream()
                .map(line -> new InvoiceLineResult(
                        line.getId(),
                        line.getInvoiceId(),
                        line.getLineType(),
                        line.getItemName(),
                        line.getReferenceId(),
                        line.getQuantity(),
                        line.getUnitPrice(),
                        line.getAmount(),
                        line.getCreatedAt()
                ))
                .toList();

        return new InvoiceResult(
                invoice.getId(),
                invoice.getInvoiceCode(),
                invoice.getVisitId(),
                invoice.getPaymentId(),
                invoice.getType(),
                invoice.getOriginalInvoiceId(),
                invoice.getAdjustmentReason(),
                invoice.getTotalAmount(),
                invoice.getCreatedBy(),
                invoice.getCreatedAt(),
                lines
        );
    }
}
