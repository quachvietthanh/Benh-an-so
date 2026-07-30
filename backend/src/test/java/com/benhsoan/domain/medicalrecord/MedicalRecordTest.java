package com.benhsoan.domain.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAlreadyLockedException;
import com.benhsoan.domain.shared.exception.ValidationException;

class MedicalRecordTest {

    private final Instant now = Instant.parse("2026-08-20T02:00:00Z");

    @Test
    void locksAndRejectsFurtherUpdates() {
        MedicalRecord record = recordWithRequiredContent();
        record.open(UUID.randomUUID(), now);
        record.lock(UUID.randomUUID(), now.plusSeconds(1));

        assertTrue(record.isLocked());
        assertThrows(MedicalRecordAlreadyLockedException.class, () -> record.updateContent(
                "Updated complaint", null, null, null, null, null, null, "Updated conclusion",
                UUID.randomUUID(), now.plusSeconds(2)
        ));
    }

    @Test
    void rejectsLockingRecordWithoutRequiredContent() {
        MedicalRecord record = MedicalRecord.create(
                UUID.randomUUID(), null, null, null, null, null, null, null, null,
                UUID.randomUUID(), now
        );

        assertThrows(ValidationException.class, () -> record.lock(UUID.randomUUID(), now));
    }

    private MedicalRecord recordWithRequiredContent() {
        return MedicalRecord.create(
                UUID.randomUUID(), "Headache", null, null, null, null, null, null, "Stable",
                UUID.randomUUID(), now
        );
    }
}
