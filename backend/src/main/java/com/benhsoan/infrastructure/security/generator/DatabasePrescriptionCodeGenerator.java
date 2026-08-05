package com.benhsoan.infrastructure.security.generator;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.port.outbound.generator.PrescriptionCodeGenerator;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabasePrescriptionCodeGenerator implements PrescriptionCodeGenerator {

    private static final String PREFIX = "RX";

    private final PrescriptionRepository prescriptionRepository;

    @Override
    public String generate() {
        return prescriptionRepository.findTopByOrderByPrescriptionCodeDesc()
                .map(Prescription::getPrescriptionCode)
                .map(this::nextCode)
                .orElse(PREFIX + "000001");
    }

    private String nextCode(String currentCode) {
        int number = Integer.parseInt(currentCode.substring(PREFIX.length()));
        return PREFIX + String.format("%06d", number + 1);
    }
}
