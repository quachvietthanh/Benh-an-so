package com.benhsoan.port.outbound.notification;

public interface AppointmentNotificationPort {

    NotificationSendResult sendAppointmentReminder(AppointmentReminderMessage message);
}
