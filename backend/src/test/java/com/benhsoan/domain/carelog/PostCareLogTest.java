package com.benhsoan.domain.carelog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.carelog.enums.ContactChannel;
import com.benhsoan.domain.carelog.enums.ContactOutcome;
import com.benhsoan.domain.carelog.enums.PatientCondition;
import com.benhsoan.domain.shared.exception.ValidationException;

class PostCareLogTest {

    private static final Instant NOW = Instant.parse("2026-08-15T08:00:00Z");
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();

    @Test
    void createKeepsOptionalLinksNullable() {
        PostCareLog careLog = PostCareLog.create(
                PATIENT_ID, null, null, ContactChannel.PHONE, NOW,
                PatientCondition.STABLE, "Bệnh nhân ổn định",
                ContactOutcome.REACHED, ACTOR, NOW);

        assertEquals(PATIENT_ID, careLog.getPatientId());
        assertNull(careLog.getReminderId());
        assertNull(careLog.getVisitId());
        assertEquals(ContactChannel.PHONE, careLog.getContactChannel());
        assertEquals(ACTOR, careLog.getPerformedBy());
    }

    @Test
    void createRejectsMissingPatientId() {
        assertThrows(ValidationException.class, () -> PostCareLog.create(
                null, null, null, ContactChannel.PHONE, NOW,
                PatientCondition.STABLE, "notes", ContactOutcome.REACHED, ACTOR, NOW));
    }

    @Test
    void createRejectsBlankCareNotes() {
        assertThrows(ValidationException.class, () -> PostCareLog.create(
                PATIENT_ID, null, null, ContactChannel.PHONE, NOW,
                PatientCondition.STABLE, "   ", ContactOutcome.REACHED, ACTOR, NOW));
    }

    @Test
    void createRejectsMissingContactedAt() {
        assertThrows(ValidationException.class, () -> PostCareLog.create(
                PATIENT_ID, null, null, ContactChannel.PHONE, null,
                PatientCondition.STABLE, "notes", ContactOutcome.REACHED, ACTOR, NOW));
    }

    @Test
    void createRejectsMissingContactChannel() {
        assertThrows(ValidationException.class, () -> PostCareLog.create(
                PATIENT_ID, null, null, null, NOW,
                PatientCondition.STABLE, "notes", ContactOutcome.REACHED, ACTOR, NOW));
    }

    @Test
    void createRejectsMissingContactOutcome() {
        assertThrows(ValidationException.class, () -> PostCareLog.create(
                PATIENT_ID, null, null, ContactChannel.PHONE, NOW,
                PatientCondition.STABLE, "notes", null, ACTOR, NOW));
    }
}
