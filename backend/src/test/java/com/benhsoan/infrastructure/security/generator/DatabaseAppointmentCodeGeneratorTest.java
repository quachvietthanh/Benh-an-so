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

    @Test
    void generatesBatchCodesSequentiallyWhenNoAppointmentExists() {
        when(appointmentRepository.findAppointmentCodeWithHighestSequence()).thenReturn(Optional.empty());

        var generator = new DatabaseAppointmentCodeGenerator(appointmentRepository);
        var codes = generator.generateBatch(3);

        assertEquals(3, codes.size());
        assertEquals("APT000001", codes.get(0));
        assertEquals("APT000002", codes.get(1));
        assertEquals("APT000003", codes.get(2));
    }

    @Test
    void generatesBatchCodesSequentiallyFromExistingHighestSequence() {
        when(appointmentRepository.findAppointmentCodeWithHighestSequence()).thenReturn(Optional.of("APT000009"));

        var generator = new DatabaseAppointmentCodeGenerator(appointmentRepository);
        var codes = generator.generateBatch(4);

        assertEquals(4, codes.size());
        assertEquals("APT000010", codes.get(0));
        assertEquals("APT000011", codes.get(1));
        assertEquals("APT000012", codes.get(2));
        assertEquals("APT000013", codes.get(3));
    }

    @Test
    void generatesEmptyListWhenBatchCountIsZeroOrNegative() {
        var generator = new DatabaseAppointmentCodeGenerator(appointmentRepository);
        assertEquals(0, generator.generateBatch(0).size());
        assertEquals(0, generator.generateBatch(-1).size());
    }
}
