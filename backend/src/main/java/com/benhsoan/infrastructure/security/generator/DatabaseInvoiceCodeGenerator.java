package com.benhsoan.infrastructure.security.generator;

import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.generator.InvoiceCodeGenerator;
import com.benhsoan.port.outbound.repository.billing.InvoiceCodeSequenceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabaseInvoiceCodeGenerator implements InvoiceCodeGenerator {

    private static final String PREFIX = "HD";

    private final InvoiceCodeSequenceRepository sequenceRepository;

    @Override
    public String generate() {
        return PREFIX + String.format(
                "%06d",
                sequenceRepository.reserveNextValue(PREFIX)
        );
    }
}
