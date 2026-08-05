package com.benhsoan.infrastructure.security.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;

@ExtendWith(MockitoExtension.class)
class DatabasePrescriptionCodeGeneratorTest {

    @Mock private PrescriptionRepository prescriptionRepository;

    @Test
    void generatesFirstPrescriptionCodeWhenNoPrescriptionExists() {
        when(prescriptionRepository.findTopByOrderByPrescriptionCodeDesc()).thenReturn(Optional.empty());

        assertEquals("RX000001", new DatabasePrescriptionCodeGenerator(prescriptionRepository).generate());
    }

    @Test
    void incrementsTheLatestPrescriptionCode() {
        UUID prescriptionId = UUID.randomUUID();
        Prescription prescription = Prescription.create(prescriptionId, "RX000009", UUID.randomUUID(), null,
                UUID.randomUUID(), Instant.parse("2026-08-05T02:00:00Z"), List.of(item(prescriptionId)));
        when(prescriptionRepository.findTopByOrderByPrescriptionCodeDesc()).thenReturn(Optional.of(prescription));

        assertEquals("RX000010", new DatabasePrescriptionCodeGenerator(prescriptionRepository).generate());
    }

    private PrescriptionItem item(UUID prescriptionId) {
        return PrescriptionItem.create(UUID.randomUUID(), prescriptionId, UUID.randomUUID(), "Medicine", "Ingredient",
                "10 mg", "tablet", "1 tablet", "Once daily", AdministrationRoute.ORAL, null, 1, null,
                Instant.parse("2026-08-05T02:00:00Z"));
    }
}
