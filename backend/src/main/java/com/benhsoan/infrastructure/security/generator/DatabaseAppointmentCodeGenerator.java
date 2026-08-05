package com.benhsoan.infrastructure.security.generator;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.appointment.Appointment;
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
                .findTopByOrderByAppointmentCodeDesc()
                .map(Appointment::getAppointmentCode)
                .map(this::nextCode)
                .orElse(PREFIX + "000001");
    }

    private String nextCode(String currentCode) {

        int number = Integer.parseInt(
                currentCode.substring(PREFIX.length())
        );

        return PREFIX + String.format("%06d", number + 1);
    }

}