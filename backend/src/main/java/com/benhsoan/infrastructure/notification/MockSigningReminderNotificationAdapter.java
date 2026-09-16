package com.benhsoan.infrastructure.notification;

import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.notification.NotificationSendResult;
import com.benhsoan.port.outbound.notification.SigningReminderMessage;
import com.benhsoan.port.outbound.notification.SigningReminderNotificationPort;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MockSigningReminderNotificationAdapter implements SigningReminderNotificationPort {

    @Override
    public NotificationSendResult sendSigningReminder(SigningReminderMessage message) {
        log.info("Mock signing reminder sent: recordId={}, doctorId={}, doctorName='{}', overdueHours={}, deadlineAt={}",
                message.medicalRecordId(), message.doctorId(), message.doctorFullName(),
                message.overdueHours(), message.deadlineAt());
        return NotificationSendResult.delivered();
    }
}
