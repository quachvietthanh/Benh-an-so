package com.benhsoan.domain.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.shared.exception.ValidationException;

class MedicalRecordAmendmentTest {

    @Test
    void rejectsBlankAmendmentReason() {
        assertThrows(ValidationException.class, () -> MedicalRecordAmendment.create(
                UUID.randomUUID(), "Corrected assessment", " ", UUID.randomUUID(),
                Instant.parse("2026-08-20T02:00:00Z")
        ));
    }
}
