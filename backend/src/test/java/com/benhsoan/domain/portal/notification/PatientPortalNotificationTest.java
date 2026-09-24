package com.benhsoan.domain.portal.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PatientPortalNotificationTest {

    private static final Instant T1 = Instant.parse("2026-10-01T01:00:00Z");
    private static final Instant T2 = Instant.parse("2026-10-01T02:00:00Z");
    private static final UUID PATIENT_ID = UUID.randomUUID();

    @Test
    void factoryMethodsSetCorrectType() {
        assertEquals(PatientPortalNotificationType.APPOINTMENT_REMINDER,
                PatientPortalNotification.reminder(PATIENT_ID, "t", "m", UUID.randomUUID(), T1).getType());
        assertEquals(PatientPortalNotificationType.APPOINTMENT_CHANGED,
                PatientPortalNotification.changed(PATIENT_ID, "t", "m", UUID.randomUUID(), UUID.randomUUID(), T1).getType());
        assertEquals(PatientPortalNotificationType.LAB_RESULT_AVAILABLE,
                PatientPortalNotification.labResultAvailable(PATIENT_ID, "t", "m", UUID.randomUUID(), T1).getType());
    }

    @Test
    void markReadIsIdempotent() {
        PatientPortalNotification notification =
                PatientPortalNotification.reminder(PATIENT_ID, "t", "m", UUID.randomUUID(), T1);

        assertFalse(notification.isRead());
        assertNull(notification.getReadAt());

        notification.markRead(T1);
        notification.markRead(T2);

        assertTrue(notification.isRead());
        assertEquals(T1, notification.getReadAt());
    }
}
