package com.benhsoan.infrastructure.security.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;

@ExtendWith(MockitoExtension.class)
class DatabaseAppointmentCodeGeneratorTest {

    @Mock private AppointmentRepository appointmentRepository;

    @Test
    void generatesFirstAppointmentCodeWhenNoAppointmentExists() {
        when(appointmentRepository.findAppointmentCodeWithHighestSequence()).thenReturn(Optional.empty());

        assertEquals("APT000001", new DatabaseAppointmentCodeGenerator(appointmentRepository).generate());
    }

    @Test
    void incrementsTheHighestNumericSequenceAcrossLegacyAndNewPrefixes() {
        when(appointmentRepository.findAppointmentCodeWithHighestSequence()).thenReturn(Optional.of("APT000009"));

        assertEquals("APT000010", new DatabaseAppointmentCodeGenerator(appointmentRepository).generate());
    }

    @Test
    void keepsWorkingWhenLegacyCodeHasTheHighestSequence() {
        when(appointmentRepository.findAppointmentCodeWithHighestSequence()).thenReturn(Optional.of("LH000123"));

        assertEquals("APT000124", new DatabaseAppointmentCodeGenerator(appointmentRepository).generate());
    }
}
