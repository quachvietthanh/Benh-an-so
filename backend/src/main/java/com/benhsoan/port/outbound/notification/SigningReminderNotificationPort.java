package com.benhsoan.port.outbound.notification;

public interface SigningReminderNotificationPort {

    NotificationSendResult sendSigningReminder(SigningReminderMessage message);
}
