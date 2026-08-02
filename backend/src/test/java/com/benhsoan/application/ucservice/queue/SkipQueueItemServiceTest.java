package com.benhsoan.application.ucservice.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.queue.MedicalQueue;
import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.queue.exception.UnauthorizedQueueOperationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.command.queue.SkipQueueItemCommand;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.outbound.repository.crudRepository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.crudRepository.queue.MedicalQueueRepository;
import com.benhsoan.port.outbound.repository.crudRepository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.repository.crudRepository.visit.VisitRepository;
import com.benhsoan.port.outbound.repository.queryRepository.queue.QueueItemQueryRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class SkipQueueItemServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-02T02:00:00Z");

    @Test
    void skipsAppointmentItemAndCancelsVisitAndAppointment() {
        TestContext context = context(true);

        QueueItemResult response = context.service.skip(
                new SkipQueueItemCommand(context.item.getId(), "Patient absent when called"));

        assertEquals(QueueItemStatus.SKIPPED, context.item.getStatus());
        assertEquals(VisitStatus.CANCELLED, context.visit.getStatus());
        assertEquals(AppointmentStatus.CANCELLED, context.appointment.getStatus());
        assertEquals(SkipQueueItemService.APPOINTMENT_CANCEL_REASON, context.appointment.getCancelReason());
        verify(context.queueItemRepository).save(context.item);
        verify(context.visitRepository).save(context.visit);
        verify(context.appointmentRepository).save(context.appointment);
        verify(context.auditService).recordSkipped(context.item, SkipQueueItemService.APPOINTMENT_CANCEL_REASON);
        assertEquals("Nguyen Van A", response.patientName());
        assertEquals("Bac si Nguyen Van B", response.doctorName());
        assertEquals("P101", response.roomNumber());
        assertEquals("VIS000100", response.visitCode());
    }

    @Test
    void skipsWalkInWithoutLoadingOrSavingAppointment() {
        TestContext context = context(false);

        context.service.skip(new SkipQueueItemCommand(context.item.getId(), "Patient absent when called"));

        assertEquals(QueueItemStatus.SKIPPED, context.item.getStatus());
        assertEquals(VisitStatus.CANCELLED, context.visit.getStatus());
        verify(context.appointmentRepository, never()).findByIdForUpdate(org.mockito.ArgumentMatchers.any());
        verify(context.appointmentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsDoctorWhoDoesNotOwnQueue() {
        UUID queueDoctorId = UUID.randomUUID();
        MedicalQueue queue = MedicalQueue.create(queueDoctorId, UUID.randomUUID(), LocalDate.of(2026, 8, 2), NOW);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        when(currentUserPort.hasRole("DOCTOR")).thenReturn(true);
        when(currentUserPort.getCurrentUserId()).thenReturn(UUID.randomUUID());

        QueueOperationAuthorization authorization = new QueueOperationAuthorization(currentUserPort);

        assertThrows(UnauthorizedQueueOperationException.class, () -> authorization.requireSkipPermission(queue));
    }

    @Test
    void allowsAdminReceptionistAndOwningDoctor() {
        UUID doctorId = UUID.randomUUID();
        MedicalQueue queue = MedicalQueue.create(doctorId, UUID.randomUUID(), LocalDate.of(2026, 8, 2), NOW);

        CurrentUserPort admin = mock(CurrentUserPort.class);
        when(admin.hasRole("ADMIN")).thenReturn(true);
        assertDoesNotThrow(() -> new QueueOperationAuthorization(admin).requireSkipPermission(queue));

        CurrentUserPort receptionist = mock(CurrentUserPort.class);
        when(receptionist.hasRole("RECEPTIONIST")).thenReturn(true);
        assertDoesNotThrow(() -> new QueueOperationAuthorization(receptionist).requireSkipPermission(queue));

        CurrentUserPort doctor = mock(CurrentUserPort.class);
        when(doctor.hasRole("DOCTOR")).thenReturn(true);
        when(doctor.getCurrentUserId()).thenReturn(doctorId);
        assertDoesNotThrow(() -> new QueueOperationAuthorization(doctor).requireSkipPermission(queue));
    }

    private TestContext context(boolean withAppointment) {
        UUID doctorId = UUID.randomUUID();
        UUID appointmentId = withAppointment ? UUID.randomUUID() : null;
        MedicalQueue queue = MedicalQueue.create(doctorId, UUID.randomUUID(), LocalDate.of(2026, 8, 2), NOW);
        Visit visit = Visit.create("VIS000100", UUID.randomUUID(), doctorId, appointmentId, null,
                withAppointment ? VisitType.APPOINTMENT : VisitType.WALK_IN,
                NOW, "Consultation", null, UUID.randomUUID(), NOW);
        visit.start(NOW.plusSeconds(30));
        QueueItem item = QueueItem.create(queue.getId(), visit.getPatientId(), appointmentId, visit.getId(),
                withAppointment ? QueueItemSourceType.APPOINTMENT : QueueItemSourceType.WALK_IN,
                1, LocalDate.of(2026, 8, 2), UUID.randomUUID(), NOW);
        item.call(NOW.plusSeconds(30));

        Appointment appointment = withAppointment
                ? Appointment.restore(appointmentId, "AP000100", visit.getPatientId(), doctorId,
                        NOW.plusSeconds(3600), NOW.plusSeconds(5400), AppointmentStatus.IN_PROGRESS,
                        "Consultation", null, NOW, null, UUID.randomUUID(), NOW)
                : null;

        QueueItemRepository queueItemRepository = mock(QueueItemRepository.class);
        MedicalQueueRepository medicalQueueRepository = mock(MedicalQueueRepository.class);
        VisitRepository visitRepository = mock(VisitRepository.class);
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        QueueAuditService auditService = mock(QueueAuditService.class);
        QueueItemQueryRepository queryRepository = mock(QueueItemQueryRepository.class);
        QueueItemResult result = new QueueItemResult(item.getId(), queue.getId(), item.getPatientId(),
                "Nguyen Van A", doctorId, "Bac si Nguyen Van B", queue.getRoomId(), "P101", appointmentId,
                visit.getId(), visit.getVisitCode(), item.getSourceType(), QueueItemStatus.SKIPPED,
                item.getQueueNumber(), item.getQueueDate(), item.getCheckedInAt(), item.getCalledAt(), null, null, null,
                NOW.plusSeconds(60), "Patient absent when called");

        when(queueItemRepository.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));
        when(medicalQueueRepository.findById(queue.getId())).thenReturn(Optional.of(queue));
        when(visitRepository.findByIdForUpdate(visit.getId())).thenReturn(Optional.of(visit));
        if (appointment != null) {
            when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
        }
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(clockPort.now()).thenReturn(NOW.plusSeconds(60));
        when(queryRepository.findDetailById(item.getId())).thenReturn(Optional.of(result));

        SkipQueueItemService service = new SkipQueueItemService(queueItemRepository, medicalQueueRepository,
                visitRepository, appointmentRepository, new QueueOperationAuthorization(currentUserPort),
                queryRepository, clockPort, auditService);
        return new TestContext(service, item, visit, appointment, queueItemRepository, visitRepository,
                appointmentRepository, auditService);
    }

    private record TestContext(
            SkipQueueItemService service,
            QueueItem item,
            Visit visit,
            Appointment appointment,
            QueueItemRepository queueItemRepository,
            VisitRepository visitRepository,
            AppointmentRepository appointmentRepository,
            QueueAuditService auditService
    ) {
    }
}
