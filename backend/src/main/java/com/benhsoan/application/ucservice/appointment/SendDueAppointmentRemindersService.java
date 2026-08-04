package com.benhsoan.application.ucservice.appointment;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.benhsoan.config.AppointmentReminderProperties;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.port.inbound.appointment.SendDueAppointmentRemindersUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendDueAppointmentRemindersService
        implements SendDueAppointmentRemindersUseCase {

    private static final Set<AppointmentStatus> REMINDABLE_STATUSES =
            Set.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED);

    private final AppointmentRepository appointmentRepository;
    private final ProcessAppointmentReminderService processAppointmentReminderService;
    private final ClockPort clockPort;
    private final AppointmentReminderProperties properties;

    @Override
    public void sendDueReminders() {
        Instant now = clockPort.now();
        Instant reminderDeadline = now.plus(properties.advanceHours(), ChronoUnit.HOURS);
        List<UUID> appointmentIds = appointmentRepository.findDueReminderIds(
                now, reminderDeadline, REMINDABLE_STATUSES);

        for (UUID appointmentId : appointmentIds) {
            try {
                processAppointmentReminderService.process(appointmentId, now);
            } catch (RuntimeException exception) {
                log.error("Unable to process appointment reminder for appointmentId={}",
                        appointmentId, exception);
            }
        }
    }
}
