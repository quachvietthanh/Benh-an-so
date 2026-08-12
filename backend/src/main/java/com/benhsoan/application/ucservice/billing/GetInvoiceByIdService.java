package com.benhsoan.application.ucservice.billing;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.billing.exception.InvoiceNotFoundException;
import com.benhsoan.port.dto.result.InvoiceResult;
import com.benhsoan.port.inbound.billing.GetInvoiceByIdUseCase;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetInvoiceByIdService implements GetInvoiceByIdUseCase {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceResultMapper resultMapper;

    @Override
    public InvoiceResult getById(UUID invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .map(resultMapper::toResult)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
    }
}
