package com.benhsoan.infrastructure.notification;

import org.springframework.stereotype.Component;

import com.benhsoan.port.outbound.notification.AppointmentNotificationPort;
import com.benhsoan.port.outbound.notification.AppointmentReminderMessage;
import com.benhsoan.port.outbound.notification.NotificationSendResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MockAppointmentNotificationAdapter
        implements AppointmentNotificationPort {

    @Override
    public NotificationSendResult sendAppointmentReminder(
            AppointmentReminderMessage message
    ) {
        log.info("Mock appointment reminder sent for appointmentCode={}",
                message.appointmentCode());
        return NotificationSendResult.delivered();
    }
}
