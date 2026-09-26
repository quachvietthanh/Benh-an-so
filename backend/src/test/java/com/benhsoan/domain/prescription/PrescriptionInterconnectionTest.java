package com.benhsoan.domain.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.prescription.enums.InterconnectionStatus;
import com.benhsoan.domain.prescription.enums.PrescriptionInterconnectionAttemptType;
import com.benhsoan.domain.prescription.enums.PrescriptionInterconnectionOutcome;
import com.benhsoan.domain.prescription.exception.PrescriptionInvalidStatusException;
import com.benhsoan.domain.shared.exception.ValidationException;

class PrescriptionInterconnectionTest {

    private static final Instant NOW = Instant.parse("2026-08-21T10:30:00Z");

    @Test
    void recordsFailureThenAllowsAFollowingSuccessfulSubmission() {
        Prescription prescription = prescription();

        prescription.markInterconnectionFailed("Gateway timeout", NOW);
        assertEquals(InterconnectionStatus.FAILED, prescription.getInterconnectionStatus());
        assertEquals("Gateway timeout", prescription.getLastInterconnectionError());

        prescription.markInterconnectionSucceeded("LT-20260821-000123", NOW.plusSeconds(1));
        assertEquals(InterconnectionStatus.SUCCESS, prescription.getInterconnectionStatus());
        assertEquals("LT-20260821-000123", prescription.getInterconnectionReceiptCode());
        assertEquals(null, prescription.getLastInterconnectionError());
    }

    @Test
    void rejectsSubmissionAfterSuccessfulInterconnection() {
        Prescription prescription = prescription();
        prescription.markInterconnectionSucceeded("LT-20260821-000123", NOW);

        assertThrows(PrescriptionInvalidStatusException.class,
                () -> prescription.markInterconnectionFailed("Gateway timeout", NOW.plusSeconds(1)));
    }

    @Test
    void requiresResultFieldsThatMatchTheAttemptOutcome() {
        assertThrows(ValidationException.class, () -> PrescriptionInterconnectionLog.create(
                UUID.randomUUID(), UUID.randomUUID(), 1,
                PrescriptionInterconnectionAttemptType.SEND,
                PrescriptionInterconnectionOutcome.SUCCESS,
                "{}", "{}", null, null, UUID.randomUUID(), NOW, NOW
        ));

        assertThrows(ValidationException.class, () -> PrescriptionInterconnectionLog.create(
                UUID.randomUUID(), UUID.randomUUID(), 1,
                PrescriptionInterconnectionAttemptType.RETRY,
                PrescriptionInterconnectionOutcome.FAILED,
                "{}", null, "LT-20260821-000123", "Gateway timeout",
                UUID.randomUUID(), NOW, NOW
        ));
    }

    private Prescription prescription() {
        UUID prescriptionId = UUID.randomUUID();
        return Prescription.create(
                prescriptionId, "RX000123", UUID.randomUUID(), null, UUID.randomUUID(), NOW,
                List.of(PrescriptionItem.create(
                        UUID.randomUUID(), prescriptionId, UUID.randomUUID(), "Paracetamol 500 mg",
                        "Paracetamol", "500 mg", "vien", "1 vien", 3,
                        AdministrationRoute.ORAL, 3, 9, null, NOW
                ))
        );
    }
}
