package com.benhsoan.infrastructure.security.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.port.outbound.repository.prescription.PrescriptionCodeSequenceRepository;

@ExtendWith(MockitoExtension.class)
class DatabasePrescriptionCodeGeneratorTest {

    @Mock private PrescriptionCodeSequenceRepository sequenceRepository;

    @Test
    void generatesFirstPrescriptionCodeWhenNoPrescriptionExists() {
        when(sequenceRepository.reserveNextValue("RX")).thenReturn(1L);

        assertEquals("RX000001", new DatabasePrescriptionCodeGenerator(sequenceRepository).generate());
    }

    @Test
    void incrementsTheLatestPrescriptionCode() {
        when(sequenceRepository.reserveNextValue("RX")).thenReturn(10L);

        assertEquals("RX000010", new DatabasePrescriptionCodeGenerator(sequenceRepository).generate());
    }
}
