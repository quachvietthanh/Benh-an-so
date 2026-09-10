package com.benhsoan.domain.prescription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.medicine.enums.AdministrationRoute;
import com.benhsoan.domain.prescription.enums.InterconnectionStatus;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.prescription.exception.PrescriptionAlreadyCancelledException;
import com.benhsoan.domain.prescription.exception.PrescriptionAlreadyDispensedException;
import com.benhsoan.domain.shared.exception.ValidationException;

class PrescriptionTest {

    private static final Instant NOW = Instant.parse("2026-08-20T02:30:00Z");

    @Test
    @DisplayName("Successfully cancels a pending dispense prescription with valid reason (QTN-27, TC-01)")
    void cancel_success() {
        Prescription prescription = createPendingPrescription();
        assertTrue(prescription.isPendingDispense());
        assertNull(prescription.getCancelReason());

        UUID cancelledBy = UUID.randomUUID();
        Instant cancelledAt = NOW.plusSeconds(300);
        String reason = "Thay đổi phương án điều trị";

        prescription.cancel(reason, cancelledBy, cancelledAt);

        assertEquals(PrescriptionStatus.CANCELLED, prescription.getStatus());
        assertEquals("Thay đổi phương án điều trị", prescription.getCancelReason());
        assertEquals(cancelledBy, prescription.getUpdatedBy());
        assertEquals(cancelledAt, prescription.getUpdatedAt());
        assertFalse(prescription.isPendingDispense());
    }

    @Test
    @DisplayName("Rejects cancellation without a reason (QTN-27, TC-02)")
    void cancel_missingReason() {
        Prescription prescription = createPendingPrescription();
        UUID cancelledBy = UUID.randomUUID();
        Instant cancelledAt = NOW.plusSeconds(300);

        assertThrows(ValidationException.class, () -> prescription.cancel(null, cancelledBy, cancelledAt));
        assertThrows(ValidationException.class, () -> prescription.cancel("", cancelledBy, cancelledAt));
        assertThrows(ValidationException.class, () -> prescription.cancel("   ", cancelledBy, cancelledAt));
        assertEquals(PrescriptionStatus.PENDING_DISPENSE, prescription.getStatus());
    }

    @Test
    @DisplayName("Rejects cancellation when prescription is already dispensed (QTN-27, TC-03)")
    void cancel_alreadyDispensed() {
        Prescription prescription = createPendingPrescription();
        prescription.markDispensed(UUID.randomUUID(), NOW.plusSeconds(100));
        assertEquals(PrescriptionStatus.DISPENSED, prescription.getStatus());

        PrescriptionAlreadyDispensedException ex = assertThrows(
                PrescriptionAlreadyDispensedException.class,
                () -> prescription.cancel("Đổi thuốc", UUID.randomUUID(), NOW.plusSeconds(200))
        );
        assertTrue(ex.getMessage().contains("dispensed"));
    }

    @Test
    @DisplayName("Rejects cancellation when prescription is already cancelled (QTN-27)")
    void cancel_alreadyCancelled() {
        Prescription prescription = createPendingPrescription();
        prescription.cancel("Lý do lần 1", UUID.randomUUID(), NOW.plusSeconds(100));
        assertEquals(PrescriptionStatus.CANCELLED, prescription.getStatus());

        assertThrows(
                PrescriptionAlreadyCancelledException.class,
                () -> prescription.cancel("Lý do lần 2", UUID.randomUUID(), NOW.plusSeconds(200))
        );
    }

    @Test
    @DisplayName("Restores prescription with cancelReason preserved")
    void restore_withCancelReason() {
        UUID id = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Prescription prescription = Prescription.restore(
                id, "RX000004", UUID.randomUUID(), PrescriptionStatus.CANCELLED,
                "Ghi chú", "Lý do hủy đơn", doctorId, NOW, doctorId, NOW.plusSeconds(60),
                InterconnectionStatus.NOT_SENT, null, null, null,
                List.of(PrescriptionItem.create(
                        UUID.randomUUID(), id, UUID.randomUUID(), "Paracetamol 500 mg",
                        "Paracetamol", "500 mg", "vien", "1 vien", 3,
                        AdministrationRoute.ORAL, 3, 9, null, NOW
                ))
        );

        assertEquals(PrescriptionStatus.CANCELLED, prescription.getStatus());
        assertEquals("Lý do hủy đơn", prescription.getCancelReason());
        assertEquals("Ghi chú", prescription.getNote());
    }

    private Prescription createPendingPrescription() {
        UUID prescriptionId = UUID.randomUUID();
        return Prescription.create(
                prescriptionId, "RX000123", UUID.randomUUID(), "Ghi chú ban đầu", UUID.randomUUID(), NOW,
                List.of(PrescriptionItem.create(
                        UUID.randomUUID(), prescriptionId, UUID.randomUUID(), "Paracetamol 500 mg",
                        "Paracetamol", "500 mg", "vien", "1 vien", 3,
                        AdministrationRoute.ORAL, 3, 9, null, NOW
                ))
        );
    }
}
