package com.benhsoan.infrastructure.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.benhsoan.port.inbound.appointment.SendDueAppointmentRemindersUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "appointment.reminder.enabled", havingValue = "true")
public class AppointmentReminderScheduler {

    private final SendDueAppointmentRemindersUseCase sendDueAppointmentRemindersUseCase;

    @Scheduled(fixedDelayString = "${appointment.reminder.scan-interval-ms}")
    public void sendDueReminders() {
        sendDueAppointmentRemindersUseCase.sendDueReminders();
    }
}
