package com.benhsoan.infrastructure.security.generator;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.generator.AppointmentCodeGenerator;
import com.benhsoan.port.outbound.repository.appointment.AppointmentCodeSequenceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabaseAppointmentCodeGenerator implements AppointmentCodeGenerator {

    private static final String PREFIX = "APT";

    private final AppointmentCodeSequenceRepository sequenceRepository;

    @Override
    public String generate() {
        long sequence = sequenceRepository.reserveNextValue(PREFIX);
        return PREFIX + String.format("%06d", sequence);
    }

    @Override
    public List<String> generateBatch(int count) {
        if (count <= 0) {
            return List.of();
        }

        long endNumber = sequenceRepository.reserveNextValues(PREFIX, count);
        long startNumber = endNumber - count + 1;

        List<String> codes = new ArrayList<>(count);
        for (long i = startNumber; i <= endNumber; i++) {
            codes.add(PREFIX + String.format("%06d", i));
        }
        return codes;
    }
}
