package com.benhsoan.infrastructure.security.generator;

import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.generator.PrescriptionCodeGenerator;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionCodeSequenceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabasePrescriptionCodeGenerator implements PrescriptionCodeGenerator {

    private static final String PREFIX = "RX";
    private static final int MIN_SEQUENCE_DIGITS = 6;
    private static final int MAX_CODE_LENGTH = 30;

    private final PrescriptionCodeSequenceRepository sequenceRepository;

    @Override
    public String generate() {
        long sequenceValue = sequenceRepository.reserveNextValue(PREFIX);
        if (sequenceValue <= 0) {
            throw new IllegalStateException(
                    "Prescription code sequence value must be positive."
            );
        }

        String prescriptionCode = PREFIX + String.format(
                "%0" + MIN_SEQUENCE_DIGITS + "d",
                sequenceValue
        );
        if (!prescriptionCode.matches("RX\\d{" + MIN_SEQUENCE_DIGITS + ",}")
                || prescriptionCode.length() > MAX_CODE_LENGTH) {
            throw new IllegalStateException(
                    "Generated prescription code has an invalid format or length."
            );
        }

        return prescriptionCode;
    }
}
