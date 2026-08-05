package com.benhsoan.application.ucservice.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.enums.AppointmentStatus;
import com.benhsoan.domain.queue.MedicalQueue;
import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.command.queue.CallNextQueueItemCommand;
import com.benhsoan.port.dto.command.queue.CompleteQueueItemCommand;
import com.benhsoan.port.dto.command.queue.UpdateQueueItemStatusCommand;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.queue.MedicalQueueRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemQueryRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

class QueueFlowServiceTest {

    @Test
    void callNextStartsQueueItemAndVisitTogether() {
        Instant now = Instant.parse("2026-07-31T02:00:00Z");
        UUID doctorId = UUID.randomUUID();
        MedicalQueue queue = MedicalQueue.create(doctorId, UUID.randomUUID(), LocalDate.of(2026, 7, 31), now);
        Visit visit = Visit.create("VIS000010", UUID.randomUUID(), doctorId, null, null, VisitType.WALK_IN,
                now, "Kham tong quat", null, UUID.randomUUID());
        QueueItem item = QueueItem.create(queue.getId(), visit.getPatientId(), null, visit.getId(),
                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 7, 31), UUID.randomUUID(), now);
        MedicalQueueRepository queueRepository = mock(MedicalQueueRepository.class);
        QueueItemRepository queueItemRepository = mock(QueueItemRepository.class);
        VisitRepository visitRepository = mock(VisitRepository.class);
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        QueueItemQueryRepository queryRepository = mock(QueueItemQueryRepository.class);
        QueueItemResult expectedResult = result(item, queue, QueueItemStatus.IN_PROGRESS);
        when(queueRepository.findByIdForUpdate(queue.getId())).thenReturn(Optional.of(queue));
        when(queueItemRepository.findNextWaitingForUpdate(queue.getId())).thenReturn(Optional.of(item));
        when(visitRepository.findByIdForUpdate(visit.getId())).thenReturn(Optional.of(visit));
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(clockPort.now()).thenReturn(now.plusSeconds(60));
        when(queryRepository.findDetailById(item.getId())).thenReturn(Optional.of(expectedResult));

        CallNextQueueItemService service = new CallNextQueueItemService(queueRepository, queueItemRepository,
                visitRepository, appointmentRepository, new QueueOperationAuthorization(currentUserPort), queryRepository, clockPort,
                mock(QueueAuditService.class));
        QueueItemResult response = service.callNext(new CallNextQueueItemCommand(queue.getId()));

        assertEquals(QueueItemStatus.IN_PROGRESS, item.getStatus());
        assertEquals(VisitStatus.IN_PROGRESS, visit.getStatus());
        assertEquals(QueueItemStatus.IN_PROGRESS, response.status());
        verify(queueItemRepository).save(item);
        verify(visitRepository).save(visit);
        assertDenormalizedFields(response);
    }

    @Test
    void completeRequiresLockedMedicalRecordAndCompletesBothAggregates() {
        Instant now = Instant.parse("2026-07-31T02:00:00Z");
        UUID doctorId = UUID.randomUUID();
        MedicalQueue queue = MedicalQueue.create(doctorId, UUID.randomUUID(), LocalDate.of(2026, 7, 31), now);
        Visit visit = Visit.create("VIS000011", UUID.randomUUID(), doctorId, null, null, VisitType.WALK_IN,
                now, "Kham tong quat", null, UUID.randomUUID());
        visit.start(now.plusSeconds(30));
        QueueItem item = QueueItem.create(queue.getId(), visit.getPatientId(), null, visit.getId(),
                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 7, 31), UUID.randomUUID(), now);
        item.call(now.plusSeconds(30));
        QueueItemRepository queueItemRepository = mock(QueueItemRepository.class);
        MedicalQueueRepository queueRepository = mock(MedicalQueueRepository.class);
        VisitRepository visitRepository = mock(VisitRepository.class);
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        MedicalRecordRepository medicalRecordRepository = mock(MedicalRecordRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        QueueItemQueryRepository queryRepository = mock(QueueItemQueryRepository.class);
        QueueItemResult expectedResult = result(item, queue, QueueItemStatus.COMPLETED);
        MedicalRecord lockedRecord = mock(MedicalRecord.class);
        when(queueItemRepository.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));
        when(queueRepository.findById(queue.getId())).thenReturn(Optional.of(queue));
        when(visitRepository.findByIdForUpdate(visit.getId())).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(visit.getId())).thenReturn(Optional.of(lockedRecord));
        when(lockedRecord.isLocked()).thenReturn(true);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(clockPort.now()).thenReturn(now.plusSeconds(60));
        when(queryRepository.findDetailById(item.getId())).thenReturn(Optional.of(expectedResult));

        CompleteQueueItemService service = new CompleteQueueItemService(queueItemRepository, queueRepository,
                visitRepository, appointmentRepository, medicalRecordRepository, new QueueOperationAuthorization(currentUserPort),
                queryRepository, clockPort, mock(QueueAuditService.class));
        QueueItemResult response = service.complete(new CompleteQueueItemCommand(item.getId()));

        assertEquals(QueueItemStatus.COMPLETED, item.getStatus());
        assertEquals(VisitStatus.COMPLETED, visit.getStatus());
        assertEquals(QueueItemStatus.COMPLETED, response.status());
        verify(queueItemRepository).save(item);
        verify(visitRepository).save(visit);
        assertDenormalizedFields(response);
    }

    @Test
    void updateStatusReturnsDenormalizedReadModel() {
        Instant now = Instant.parse("2026-07-31T02:00:00Z");
        UUID doctorId = UUID.randomUUID();
        MedicalQueue queue = MedicalQueue.create(doctorId, UUID.randomUUID(), LocalDate.of(2026, 7, 31), now);
        Visit visit = Visit.create("VIS000010", UUID.randomUUID(), doctorId, null, null, VisitType.WALK_IN,
                now, "Kham tong quat", null, UUID.randomUUID());
        visit.start(now.plusSeconds(30));
        QueueItem item = QueueItem.create(queue.getId(), visit.getPatientId(), null, visit.getId(),
                QueueItemSourceType.WALK_IN, 1, LocalDate.of(2026, 7, 31), UUID.randomUUID(), now);
        item.call(now.plusSeconds(30));

        QueueItemRepository queueItemRepository = mock(QueueItemRepository.class);
        MedicalQueueRepository queueRepository = mock(MedicalQueueRepository.class);
        VisitRepository visitRepository = mock(VisitRepository.class);
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        QueueItemQueryRepository queryRepository = mock(QueueItemQueryRepository.class);
        QueueItemResult expectedResult = result(item, queue, QueueItemStatus.WAITING_FOR_RESULT);
        when(queueItemRepository.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));
        when(queueRepository.findById(queue.getId())).thenReturn(Optional.of(queue));
        when(visitRepository.findByIdForUpdate(visit.getId())).thenReturn(Optional.of(visit));
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(clockPort.now()).thenReturn(now.plusSeconds(60));
        when(queryRepository.findDetailById(item.getId())).thenReturn(Optional.of(expectedResult));

        UpdateQueueItemStatusService service = new UpdateQueueItemStatusService(queueItemRepository,
                queueRepository, visitRepository, appointmentRepository, new QueueOperationAuthorization(currentUserPort),
                queryRepository, clockPort, mock(QueueAuditService.class));
        QueueItemResult response = service.updateStatus(
                new UpdateQueueItemStatusCommand(item.getId(), QueueItemStatus.WAITING_FOR_RESULT, null));

        assertEquals(QueueItemStatus.WAITING_FOR_RESULT, item.getStatus());
        assertEquals(VisitStatus.WAITING_FOR_RESULT, visit.getStatus());
        assertEquals(QueueItemStatus.WAITING_FOR_RESULT, response.status());
        assertDenormalizedFields(response);
    }

    @Test
    void cancellingAppointmentQueueItemAlsoCancelsVisitAndAppointment() {
        Instant now = Instant.parse("2026-07-31T02:00:00Z");
        UUID doctorId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        MedicalQueue queue = MedicalQueue.create(doctorId, UUID.randomUUID(), LocalDate.of(2026, 7, 31), now);
        Visit visit = Visit.create("VIS000012", UUID.randomUUID(), doctorId, appointmentId, null,
                VisitType.APPOINTMENT, now, "Kham tong quat", null, UUID.randomUUID());
        QueueItem item = QueueItem.create(queue.getId(), visit.getPatientId(), appointmentId, visit.getId(),
                QueueItemSourceType.APPOINTMENT, 1, LocalDate.of(2026, 7, 31), UUID.randomUUID(), now);
        Appointment appointment = Appointment.restore(appointmentId, "AP000012", visit.getPatientId(), doctorId,
                now.plusSeconds(1800), now.plusSeconds(3600), AppointmentStatus.CHECKED_IN, "Kham tong quat",
                null, now, null, UUID.randomUUID(), now.minusSeconds(3600));

        QueueItemRepository queueItemRepository = mock(QueueItemRepository.class);
        MedicalQueueRepository queueRepository = mock(MedicalQueueRepository.class);
        VisitRepository visitRepository = mock(VisitRepository.class);
        AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
        CurrentUserPort currentUserPort = mock(CurrentUserPort.class);
        ClockPort clockPort = mock(ClockPort.class);
        QueueItemQueryRepository queryRepository = mock(QueueItemQueryRepository.class);
        when(queueItemRepository.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));
        when(queueRepository.findById(queue.getId())).thenReturn(Optional.of(queue));
        when(visitRepository.findByIdForUpdate(visit.getId())).thenReturn(Optional.of(visit));
        when(appointmentRepository.findByIdForUpdate(appointmentId)).thenReturn(Optional.of(appointment));
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(clockPort.now()).thenReturn(now.plusSeconds(60));
        when(queryRepository.findDetailById(item.getId()))
                .thenReturn(Optional.of(result(item, queue, QueueItemStatus.CANCELLED)));

        UpdateQueueItemStatusService service = new UpdateQueueItemStatusService(queueItemRepository,
                queueRepository, visitRepository, appointmentRepository, new QueueOperationAuthorization(currentUserPort),
                queryRepository, clockPort, mock(QueueAuditService.class));
        service.updateStatus(new UpdateQueueItemStatusCommand(item.getId(), QueueItemStatus.CANCELLED,
                "Patient requested cancellation"));

        assertEquals(QueueItemStatus.CANCELLED, item.getStatus());
        assertEquals(VisitStatus.CANCELLED, visit.getStatus());
        assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
        verify(appointmentRepository).save(appointment);
    }

    private QueueItemResult result(QueueItem item, MedicalQueue queue, QueueItemStatus status) {
        return new QueueItemResult(item.getId(), queue.getId(), item.getPatientId(), "Nguyen Van A",
                queue.getDoctorId(), "Bac si Nguyen Van B", queue.getRoomId(), "P101",
                item.getAppointmentId(), item.getVisitId(), "VIS000010", item.getSourceType(), status,
                item.getQueueNumber(), item.getQueueDate(), item.getCheckedInAt(), item.getCalledAt(),
                item.getCompletedAt(), item.getCancelledAt(), item.getCancelReason(), item.getSkippedAt(),
                item.getSkipReason());
    }

    private void assertDenormalizedFields(QueueItemResult result) {
        assertEquals("Nguyen Van A", result.patientName());
        assertEquals("Bac si Nguyen Van B", result.doctorName());
        assertEquals("P101", result.roomNumber());
        assertEquals("VIS000010", result.visitCode());
    }
}
