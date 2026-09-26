package com.benhsoan.infrastructure.security.generator;

import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.generator.AppointmentSeriesCodeGenerator;
import com.benhsoan.port.outbound.repository.appointment.AppointmentCodeSequenceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DatabaseAppointmentSeriesCodeGenerator implements AppointmentSeriesCodeGenerator {

    private static final String PREFIX = "SER";

    private final AppointmentCodeSequenceRepository sequenceRepository;

    @Override
    public String generate() {
        long sequence = sequenceRepository.reserveNextValue(PREFIX);
        return PREFIX + String.format("%06d", sequence);
    }
}
