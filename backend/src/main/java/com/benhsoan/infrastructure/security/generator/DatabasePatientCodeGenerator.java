package com.benhsoan.infrastructure.security.generator;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern SEQUENCE_PATTERN =
            Pattern.compile("^" + PREFIX + "(\\d+)$");

    private final PatientRepository patientRepository;

    @Override
    public String generate() {

        Optional<Patient> patient =
                patientRepository.findTopByOrderByPatientCodeDesc();

        int sequence = patient
                .map(Patient::getPatientCode)
                .map(this::parseSequence)
                .orElse(0);

        int next = sequence + 1;

        String candidate;
        do {
            candidate = format(next++);
        } while (patientRepository.existsByPatientCode(candidate));

        return candidate;
    }

    private int parseSequence(String patientCode) {
        if (patientCode == null || patientCode.isBlank()) {
            return 0;
        }

        Matcher matcher = SEQUENCE_PATTERN.matcher(patientCode.trim());
        if (!matcher.matches()) {
            return 0;
        }

        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String format(int sequence) {
        return PREFIX + String.format("%0" + CODE_LENGTH + "d", sequence);
    }
}