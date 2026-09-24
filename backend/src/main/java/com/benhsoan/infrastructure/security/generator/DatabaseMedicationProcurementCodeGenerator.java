package com.benhsoan.infrastructure.security.generator;

import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.generator.MedicationProcurementCodeGenerator;
import com.benhsoan.port.outbound.repository.inventory.MedicationProcurementCodeSequenceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabaseMedicationProcurementCodeGenerator implements MedicationProcurementCodeGenerator {

    private static final String PREFIX = "DT";

    private final MedicationProcurementCodeSequenceRepository sequenceRepository;

    @Override
    public String generate() {
        return PREFIX + String.format(
                "%06d",
                sequenceRepository.reserveNextValue(PREFIX)
        );
    }
}
