package com.benhsoan.infrastructure.security.generator;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.generator.AppointmentCodeGenerator;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabaseAppointmentCodeGenerator
        implements AppointmentCodeGenerator {

    private static final String PREFIX = "APT";

    private final AppointmentRepository appointmentRepository;

    @Override
    public String generate() {

        return appointmentRepository
                .findAppointmentCodeWithHighestSequence()
                .map(this::nextCode)
                .orElse(PREFIX + "000001");
    }

    @Override
    public List<String> generateBatch(int count) {
        if (count <= 0) {
            return List.of();
        }

        int startNumber = appointmentRepository
                .findAppointmentCodeWithHighestSequence()
                .map(code -> Integer.parseInt(code.substring(code.length() - 6)))
                .orElse(0);

        List<String> codes = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            codes.add(PREFIX + String.format("%06d", startNumber + i));
        }
        return codes;
    }

    private String nextCode(String currentCode) {

        int number = Integer.parseInt(currentCode.substring(currentCode.length() - 6));

        return PREFIX + String.format("%06d", number + 1);
    }

}
