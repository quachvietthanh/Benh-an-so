package com.benhsoan.domain.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.clinical.enums.MedicalAttachmentType;
import com.benhsoan.domain.shared.exception.ValidationException;

class MedicalAttachmentTest {

    @Test
    void createsAttachmentForClinicalResult() {
        UUID clinicalResultId = UUID.randomUUID();

        MedicalAttachment attachment = MedicalAttachment.create(
                UUID.randomUUID(), null, clinicalResultId, "result.pdf", "result.pdf",
                "clinical/result.pdf", "application/pdf", 100L, null,
                MedicalAttachmentType.LAB_RESULT, UUID.randomUUID(), Instant.parse("2026-08-20T02:00:00Z")
        );

        assertEquals(clinicalResultId, attachment.getClinicalResultId());
    }

    @Test
    void rejectsAttachmentWithBothOwners() {
        assertThrows(ValidationException.class, () -> MedicalAttachment.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "result.pdf", "result.pdf",
                "clinical/result.pdf", "application/pdf", 100L, null,
                MedicalAttachmentType.LAB_RESULT, UUID.randomUUID(), Instant.parse("2026-08-20T02:00:00Z")
        ));
    }
}
