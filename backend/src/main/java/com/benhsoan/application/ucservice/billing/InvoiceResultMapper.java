package com.benhsoan.application.ucservice.billing;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.port.dto.result.InvoiceLineResult;
import com.benhsoan.port.dto.result.InvoiceResult;
import com.benhsoan.port.dto.result.PaymentDetailResult;

@Component
public class InvoiceResultMapper {

    public InvoiceResult toResult(Invoice invoice) {
        return toResult(invoice, null);
    }

    public InvoiceResult toResult(Invoice invoice, PaymentDetailResult payment) {
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
                invoice.getDiscountAmount(),
                invoice.getDiscountRequestId(),
                invoice.getTotalAmount(),
                invoice.getCreatedBy(),
                invoice.getCreatedAt(),
                invoice.getReprintCount(),
                invoice.getLastReprintedAt(),
                lines,
                payment
        );
    }
}
