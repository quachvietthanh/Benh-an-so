package com.benhsoan.infrastructure.security.generator;

import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.generator.PrescriptionCodeGenerator;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionCodeSequenceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabasePrescriptionCodeGenerator implements PrescriptionCodeGenerator {

    private static final String PREFIX = "RX";

    private final PrescriptionCodeSequenceRepository sequenceRepository;

    @Override
    public String generate() {
        return PREFIX + String.format(
                "%06d",
                sequenceRepository.reserveNextValue(PREFIX)
        );
    }
}
