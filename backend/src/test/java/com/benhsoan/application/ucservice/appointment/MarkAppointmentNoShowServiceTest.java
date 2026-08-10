package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.queue.exception.CheckInConflictException;
import com.benhsoan.port.dto.command.appointment.MarkAppointmentNoShowCommand;
import com.benhsoan.port.dto.result.AppointmentResult;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

class MarkAppointmentNoShowServiceTest {

    @Test
    void marksScheduledAppointmentAsNoShow() {
        UUID appointmentId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Instant markedAt = Instant.parse("2026-08-09T10:00:00Z");
        Appointment appointment = Appointment.restore(
                appointmentId,
                "APT000300",
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-08-09T09:30:00Z"),
                Instant.parse("2026-08-09T10:00:00Z"),
                AppointmentStatus.SCHEDULED,
                "Tai kham",
                null,
                null,
                null,
                actorId,
                Instant.parse("2026-08-08T02:00:00Z")
        );
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        QueueItemRepository queueItemRepository = mock(QueueItemRepository.class);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
        when(queueItemRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());
        when(appointmentRepository.save(appointment)).thenReturn(appointment);

        AppointmentResult result = new MarkAppointmentNoShowService(
                appointmentRepository,
                currentUserPort,
                auditLogRepository,
                new AppointmentResultMapper(),
                queueItemRepository
        ).execute(MarkAppointmentNoShowCommand.builder()
                .appointmentId(appointmentId)
                .markedAt(markedAt)
                .build());

        assertEquals(AppointmentStatus.NO_SHOW, result.status());
        verify(auditLogRepository).save(any());
    }

    @Test
    void rejectsNoShowWhenAppointmentAlreadyHasQueueItem() {
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = Appointment.restore(
                appointmentId,
                "APT000301",
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.parse("2026-08-09T09:30:00Z"),
                Instant.parse("2026-08-09T10:00:00Z"),
                AppointmentStatus.SCHEDULED,
                "Tai kham",
                null,
                null,
                null,
                UUID.randomUUID(),
                Instant.parse("2026-08-08T02:00:00Z")
        );
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        QueueItemRepository queueItemRepository = mock(QueueItemRepository.class);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
        when(queueItemRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.of(mock(com.benhsoan.domain.queue.QueueItem.class)));

        assertThrows(CheckInConflictException.class, () -> new MarkAppointmentNoShowService(
                appointmentRepository,
                currentUserPort,
                auditLogRepository,
                new AppointmentResultMapper(),
                queueItemRepository
        ).execute(MarkAppointmentNoShowCommand.builder()
                .appointmentId(appointmentId)
                .markedAt(Instant.parse("2026-08-09T10:00:00Z"))
                .build()));

        verify(appointmentRepository, never()).save(any());
    }
}
