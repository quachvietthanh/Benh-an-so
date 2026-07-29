package com.benhsoan.application.ucservice.appointment;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.config.AppointmentReminderProperties;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.port.outbound.repository.crudRepository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class SendDueAppointmentRemindersServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-19T02:00:00Z");

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ProcessAppointmentReminderService processAppointmentReminderService;
    @Mock private ClockPort clockPort;

    @Test
    void queriesDueWindowUsingFixedClockAndProcessesEachAppointment() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        SendDueAppointmentRemindersService service = service();
        when(clockPort.now()).thenReturn(NOW);
        when(appointmentRepository.findDueReminderIds(eq(NOW), eq(NOW.plusSeconds(86_400)),
                org.mockito.ArgumentMatchers.anySet())).thenReturn(List.of(firstId, secondId));

        service.sendDueReminders();

        verify(processAppointmentReminderService).process(firstId, NOW);
        verify(processAppointmentReminderService).process(secondId, NOW);
    }

    @Test
    void continuesWhenOneAppointmentProcessingFails() {
        UUID failingId = UUID.randomUUID();
        UUID validId = UUID.randomUUID();
        SendDueAppointmentRemindersService service = service();
        when(clockPort.now()).thenReturn(NOW);
        when(appointmentRepository.findDueReminderIds(eq(NOW), eq(NOW.plusSeconds(86_400)),
                org.mockito.ArgumentMatchers.anySet())).thenReturn(List.of(failingId, validId));
        doThrow(new IllegalStateException("Persistence failure"))
                .when(processAppointmentReminderService).process(failingId, NOW);

        service.sendDueReminders();

        verify(processAppointmentReminderService).process(validId, NOW);
    }

    @Test
    void runsNormallyWhenNoAppointmentIsDue() {
        SendDueAppointmentRemindersService service = service();
        when(clockPort.now()).thenReturn(NOW);
        when(appointmentRepository.findDueReminderIds(eq(NOW), eq(NOW.plusSeconds(86_400)),
                org.mockito.ArgumentMatchers.anySet())).thenReturn(List.of());

        service.sendDueReminders();

        verify(appointmentRepository).findDueReminderIds(eq(NOW), eq(NOW.plusSeconds(86_400)),
                org.mockito.ArgumentMatchers.argThat(statuses -> statuses.containsAll(
                        List.of(AppointmentStatus.SCHEDULED, AppointmentStatus.CONFIRMED))));
    }

    private SendDueAppointmentRemindersService service() {
        return new SendDueAppointmentRemindersService(appointmentRepository,
                processAppointmentReminderService, clockPort,
                new AppointmentReminderProperties(true, 24, 60_000,
                        ZoneId.of("Asia/Ho_Chi_Minh")));
    }
}
