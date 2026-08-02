package com.benhsoan.application.ucservice.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.command.appointment.CancelAppointmentCommand;
import com.benhsoan.port.outbound.repository.crudRepository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.crudRepository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.repository.crudRepository.visit.VisitRepository;
import com.benhsoan.port.outbound.repository.logRepository.AuditLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class CancelAppointmentServiceTest {

    @Test
    void cancelsCheckedInFlowUsingQueueFirstLockOrder() {
        Instant now = Instant.parse("2026-08-02T02:00:00Z");
        UUID appointmentId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Visit visit = Visit.create("VIS000200", patientId, doctorId, appointmentId, null,
                VisitType.APPOINTMENT, now, "Consultation", null, actorId, now);
        QueueItem item = QueueItem.create(UUID.randomUUID(), patientId, appointmentId, visit.getId(),
                QueueItemSourceType.APPOINTMENT, 1, LocalDate.of(2026, 8, 2), actorId, now);
        Appointment appointment = Appointment.restore(appointmentId, "AP000200", patientId, doctorId,
                now.plusSeconds(1800), now.plusSeconds(3600), AppointmentStatus.CHECKED_IN, "Consultation",
                null, now, null, actorId, now.minusSeconds(3600));

        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        QueueItemRepository queueItemRepository = mock(QueueItemRepository.class);
        VisitRepository visitRepository = mock(VisitRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(actorId);
        when(queueItemRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.of(item));
        when(queueItemRepository.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));
        when(visitRepository.findByIdForUpdate(visit.getId())).thenReturn(Optional.of(visit));
        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(appointment)).thenReturn(appointment);
        when(clockPort.now()).thenReturn(now.plusSeconds(60));

        CancelAppointmentService service = new CancelAppointmentService(appointmentRepository, currentUserPort,
                new AppointmentResultMapper(), auditLogRepository, queueItemRepository, visitRepository, clockPort);
        service.cancel(appointmentId, new CancelAppointmentCommand("Patient requested cancellation"));

        assertEquals(QueueItemStatus.CANCELLED, item.getStatus());
        assertEquals(VisitStatus.CANCELLED, visit.getStatus());
        assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
        InOrder lockOrder = inOrder(queueItemRepository, visitRepository, appointmentRepository);
        lockOrder.verify(queueItemRepository).findByIdForUpdate(item.getId());
        lockOrder.verify(visitRepository).findByIdForUpdate(visit.getId());
        lockOrder.verify(appointmentRepository).findByIdForUpdate(appointmentId);
        verify(auditLogRepository).save(any());
    }
}
