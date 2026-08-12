package com.benhsoan.infrastructure.security.generator;

import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.generator.AdjustmentInvoiceCodeGenerator;
import com.benhsoan.port.outbound.repository.billing.InvoiceCodeSequenceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabaseAdjustmentInvoiceCodeGenerator
        implements AdjustmentInvoiceCodeGenerator {

    private static final String PREFIX = "HDDC";

    private final InvoiceCodeSequenceRepository sequenceRepository;

    @Override
    public String generate() {
        return PREFIX + String.format(
                "%06d",
                sequenceRepository.reserveNextValue(PREFIX)
        );
    }
}
