package com.benhsoan.infrastructure.security.generator;

import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.generator.AppointmentSeriesCodeGenerator;
import com.benhsoan.port.outbound.repository.appointment.AppointmentSeriesRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabaseAppointmentSeriesCodeGenerator implements AppointmentSeriesCodeGenerator {

    private static final String PREFIX = "SER";

    private final AppointmentSeriesRepository appointmentSeriesRepository;

    @Override
    public String generate() {
        return appointmentSeriesRepository
                .findHighestSeriesCode()
                .map(this::nextCode)
                .orElse(PREFIX + "000001");
    }

    private String nextCode(String currentCode) {
        int number = Integer.parseInt(currentCode.substring(currentCode.length() - 6));
        return PREFIX + String.format("%06d", number + 1);
    }
}
