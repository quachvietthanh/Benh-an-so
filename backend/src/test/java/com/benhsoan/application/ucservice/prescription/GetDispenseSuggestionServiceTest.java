package com.benhsoan.application.ucservice.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.enums.InterconnectionStatus;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.DispenseSuggestionResult;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class GetDispenseSuggestionServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T02:00:00Z");
    private static final UUID MEDICINE_ID = UUID.randomUUID();

    private final PrescriptionRepository prescriptionRepository = mock(PrescriptionRepository.class);
    private final MedicineBatchRepository medicineBatchRepository = mock(MedicineBatchRepository.class);
    private final CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    private GetDispenseSuggestionService service;

    @BeforeEach
    void setUp() {
        service = new GetDispenseSuggestionService(
                prescriptionRepository, medicineBatchRepository, currentUserPort, clockPort);
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(true);
        when(clockPort.now()).thenReturn(NOW);
    }

    @Test
    void suggestsBatchesInFefoOrder() {
        UUID prescriptionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        PrescriptionItem item = PrescriptionItem.restore(
                itemId, prescriptionId, MEDICINE_ID, "Paracetamol", "Paracetamol",
                "500 mg", "vien", "1 vien", 2, AdministrationRoute.ORAL, 5,
                20, 0, null, NOW, null);
        Prescription prescription = Prescription.restore(
                prescriptionId, "RX-001", UUID.randomUUID(), PrescriptionStatus.PENDING_DISPENSE,
                "note", null, UUID.randomUUID(), NOW, null, null,
                InterconnectionStatus.NOT_SENT, null, null, null, List.of(item));

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));

        UUID fefoBatchId = UUID.randomUUID();
        UUID laterBatchId = UUID.randomUUID();
        when(medicineBatchRepository.findAvailableByMedicineId(MEDICINE_ID, LocalDate.of(2026, 8, 20)))
                .thenReturn(List.of(
                        batch(fefoBatchId, "BATCH-A", LocalDate.of(2026, 12, 1), 8),
                        batch(laterBatchId, "BATCH-B", LocalDate.of(2027, 6, 1), 100)));

        DispenseSuggestionResult result = service.getSuggestion(prescriptionId);

        assertEquals(1, result.items().size());
        var suggestion = result.items().get(0);
        assertEquals(20, suggestion.remainingQuantity());
        assertEquals(2, suggestion.batches().size());
        assertEquals(fefoBatchId, suggestion.batches().get(0).batchId());
        assertEquals(8, suggestion.batches().get(0).suggestedQuantity());
        assertEquals(12, suggestion.batches().get(1).suggestedQuantity());
    }

    @Test
    void rejectsNonPharmacist() {
        when(currentUserPort.hasRole("PHARMACIST")).thenReturn(false);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.getSuggestion(UUID.randomUUID()));
    }

    @Test
    void rejectsMissingPrescriptionId() {
        assertThrows(ValidationException.class, () -> service.getSuggestion(null));
    }

    private MedicineBatch batch(UUID id, String batchNumber, LocalDate expiryDate, int quantity) {
        return MedicineBatch.restore(
                id, MEDICINE_ID, batchNumber, expiryDate, quantity,
                BatchStatus.ACTIVE, NOW.minusSeconds(3600), null);
    }
}
