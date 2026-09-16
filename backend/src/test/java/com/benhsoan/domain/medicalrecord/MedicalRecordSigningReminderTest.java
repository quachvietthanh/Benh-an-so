package com.benhsoan.domain.medicalrecord;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.shared.exception.ValidationException;

class MedicalRecordSigningReminderTest {

    private static final UUID RECORD_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID MANAGER_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-16T10:00:00Z");

    @Test
    @DisplayName("Khởi tạo reminder hợp lệ với status mặc định là SENT")
    void createValidReminder() {
        MedicalRecordSigningReminder reminder = MedicalRecordSigningReminder.create(
                RECORD_ID,
                DOCTOR_ID,
                MANAGER_ID,
                NOW,
                5,
                "MOCK",
                "Nhắc ký bệnh án"
        );

        assertNotNull(reminder.getId());
        assertEquals(RECORD_ID, reminder.getMedicalRecordId());
        assertEquals(DOCTOR_ID, reminder.getDoctorId());
        assertEquals(MANAGER_ID, reminder.getRemindedBy());
        assertEquals(NOW, reminder.getRemindedAt());
        assertEquals(5, reminder.getOverdueHours());
        assertEquals("MOCK", reminder.getChannel());
        assertEquals("Nhắc ký bệnh án", reminder.getNotes());
        assertEquals("SENT", reminder.getStatus());
    }

    @Test
    @DisplayName("Từ chối overdueHours âm")
    void rejectsNegativeOverdueHours() {
        assertThrows(ValidationException.class, () -> MedicalRecordSigningReminder.create(
                RECORD_ID,
                DOCTOR_ID,
                MANAGER_ID,
                NOW,
                -1,
                "MOCK",
                "Invalid"
        ));
    }

    @Test
    @DisplayName("Khôi phục reminder từ persistence")
    void restoreReminder() {
        UUID reminderId = UUID.randomUUID();
        MedicalRecordSigningReminder reminder = MedicalRecordSigningReminder.restore(
                reminderId,
                RECORD_ID,
                DOCTOR_ID,
                MANAGER_ID,
                NOW,
                10,
                "SMS",
                "Gửi qua SMS",
                "SENT"
        );

        assertEquals(reminderId, reminder.getId());
        assertEquals("SMS", reminder.getChannel());
        assertEquals("SENT", reminder.getStatus());
    }
}
