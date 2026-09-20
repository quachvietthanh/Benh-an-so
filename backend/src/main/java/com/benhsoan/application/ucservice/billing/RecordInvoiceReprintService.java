package com.benhsoan.application.ucservice.billing;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.billing.Invoice;
import com.benhsoan.domain.billing.exception.InvoiceNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.InvoiceResult;
import com.benhsoan.port.inbound.billing.RecordInvoiceReprintUseCase;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RecordInvoiceReprintService implements RecordInvoiceReprintUseCase {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceResultMapper resultMapper;
    private final ClockPort clockPort;

    @Override
    public InvoiceResult recordReprint(UUID invoiceId) {
        if (invoiceId == null) {
            throw new ValidationException("Invoice id is required.");
        }

        Instant now = clockPort.now();
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        invoice.recordReprint(now);
        return resultMapper.toResult(invoiceRepository.save(invoice));
    }
}
