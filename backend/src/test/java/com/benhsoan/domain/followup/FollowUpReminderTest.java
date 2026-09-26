package com.benhsoan.domain.followup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.followup.enums.ReminderStatus;
import com.benhsoan.domain.followup.enums.ReminderType;
import com.benhsoan.domain.followup.exception.FollowUpReminderInvalidStatusException;
import com.benhsoan.domain.shared.exception.ValidationException;

class FollowUpReminderTest {

    private static final Instant NOW = Instant.parse("2026-08-15T08:00:00Z");
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();

    @Test
    void createStartsPending() {
        FollowUpReminder reminder = FollowUpReminder.create(
                PATIENT_ID, null, null, LocalDate.of(2026, 8, 30), NOW,
                ReminderType.REVISIT, "Recheck", ACTOR, NOW);

        assertEquals(ReminderStatus.PENDING, reminder.getStatus());
        assertTrue(reminder.isPending());
        assertEquals(PATIENT_ID, reminder.getPatientId());
    }

    @Test
    void createRejectsMissingFollowUpDate() {
        assertThrows(ValidationException.class, () -> FollowUpReminder.create(
                PATIENT_ID, null, null, null, NOW, ReminderType.GENERAL, null, ACTOR, NOW));
    }

    @Test
    void createRejectsMissingRemindAt() {
        assertThrows(ValidationException.class, () -> FollowUpReminder.create(
                PATIENT_ID, null, null, LocalDate.of(2026, 8, 30), null, ReminderType.GENERAL, null, ACTOR, NOW));
    }

    @Test
    void createRejectsMissingPatientId() {
        assertThrows(ValidationException.class, () -> FollowUpReminder.create(
                null, null, null, LocalDate.of(2026, 8, 30), NOW, ReminderType.GENERAL, null, ACTOR, NOW));
    }

    @Test
    void updateStatusTransitionsPendingToSentThenCompleted() {
        FollowUpReminder reminder = FollowUpReminder.create(
                PATIENT_ID, null, null, LocalDate.of(2026, 8, 30), NOW,
                ReminderType.GENERAL, null, ACTOR, NOW);

        reminder.updateStatus(ReminderStatus.SENT);
        assertEquals(ReminderStatus.SENT, reminder.getStatus());

        reminder.updateStatus(ReminderStatus.COMPLETED);
        assertEquals(ReminderStatus.COMPLETED, reminder.getStatus());
    }

    @Test
    void updateStatusRejectsSettingBackToPending() {
        FollowUpReminder reminder = FollowUpReminder.create(
                PATIENT_ID, null, null, LocalDate.of(2026, 8, 30), NOW,
                ReminderType.GENERAL, null, ACTOR, NOW);

        assertThrows(ValidationException.class, () -> reminder.updateStatus(ReminderStatus.PENDING));
    }

    @Test
    void updateStatusRejectsTerminalReminder() {
        FollowUpReminder reminder = FollowUpReminder.restore(
                UUID.randomUUID(), PATIENT_ID, null, null,
                LocalDate.of(2026, 8, 30), NOW, ReminderType.GENERAL,
                ReminderStatus.CANCELLED, null, ACTOR, NOW);

        assertThrows(FollowUpReminderInvalidStatusException.class,
                () -> reminder.updateStatus(ReminderStatus.SENT));
    }
}
