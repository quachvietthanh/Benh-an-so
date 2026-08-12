package com.benhsoan.application.ucservice.billing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.port.dto.result.PayableEncounterResult;
import com.benhsoan.port.inbound.billing.GetPayableEncountersUseCase;
import com.benhsoan.port.outbound.repository.billing.InvoiceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPayableEncountersService implements GetPayableEncountersUseCase {

    private final InvoiceRepository invoiceRepository;
    private final PayableEncounterResultMapper resultMapper;

    @Override
    public Page<PayableEncounterResult> get(Pageable pageable) {
        return invoiceRepository.findPayableEncounters(pageable)
                .map(resultMapper::toResult);
    }
}
