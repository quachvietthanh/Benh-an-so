package com.benhsoan.infrastructure.security.generator;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.patient.Patient;
import com.benhsoan.port.outbound.generator.PatientCodeGenerator;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabasePatientCodeGenerator
        implements PatientCodeGenerator {

    private static final String PREFIX = "BN";

    private static final int CODE_LENGTH = 6;

    private final PatientRepository patientRepository;

    @Override
    public String generate() {

        Optional<Patient> patient =
                patientRepository.findTopByOrderByPatientCodeDesc();

        if (patient.isEmpty()) {
            return PREFIX + "000001";
        }

        String lastCode = patient.get().getPatientCode();

        int number = Integer.parseInt(
                lastCode.substring(PREFIX.length())
        );

        return PREFIX + String.format(
                "%0" + CODE_LENGTH + "d",
                number + 1
        );
    }
}