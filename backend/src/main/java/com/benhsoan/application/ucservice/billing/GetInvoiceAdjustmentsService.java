package com.benhsoan.application.ucservice.billing;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.billing.enums.InvoiceType;
import com.benhsoan.domain.billing.exception.InvoiceNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.InvoiceAdjustmentsResult;
import com.benhsoan.port.dto.result.InvoiceResult;
import com.benhsoan.port.inbound.billing.GetInvoiceAdjustmentsUseCase;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetInvoiceAdjustmentsService implements GetInvoiceAdjustmentsUseCase {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceResultMapper resultMapper;

    @Override
    public InvoiceAdjustmentsResult getAdjustments(UUID invoiceId) {
        if (invoiceId == null) {
            throw new ValidationException("Invoice id is required.");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        if (invoice.getType() != InvoiceType.ORIGINAL) {
            throw new ValidationException("Only original invoices have adjustment history.");
        }

        List<InvoiceResult> adjustments = invoiceRepository
                .findAdjustmentsByOriginalInvoiceId(invoiceId)
                .stream()
                .map(resultMapper::toResult)
                .toList();

        BigDecimal adjustmentTotal = adjustments.stream()
                .map(InvoiceResult::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finalAmount = invoice.getTotalAmount().add(adjustmentTotal);

        return new InvoiceAdjustmentsResult(
                invoiceId,
                invoice.getTotalAmount(),
                finalAmount,
                adjustments
        );
    }
}
