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
import com.benhsoan.domain.queue.MedicalQueue;
import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.command.queue.CallNextQueueItemCommand;
import com.benhsoan.port.dto.command.queue.CompleteQueueItemCommand;
import com.benhsoan.port.outbound.repository.crudRepository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.crudRepository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.crudRepository.queue.MedicalQueueRepository;
import com.benhsoan.port.outbound.repository.crudRepository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.repository.crudRepository.visit.VisitRepository;
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
        when(queueRepository.findByIdForUpdate(queue.getId())).thenReturn(Optional.of(queue));
        when(queueItemRepository.findNextWaitingForUpdate(queue.getId())).thenReturn(Optional.of(item));
        when(visitRepository.findByIdForUpdate(visit.getId())).thenReturn(Optional.of(visit));
        when(currentUserPort.hasRole("ADMIN")).thenReturn(false);
        when(currentUserPort.hasRole("RECEPTIONIST")).thenReturn(true);
        when(clockPort.now()).thenReturn(now.plusSeconds(60));

        CallNextQueueItemService service = new CallNextQueueItemService(queueRepository, queueItemRepository,
                visitRepository, appointmentRepository, new QueueOperationAuthorization(currentUserPort), new QueueItemResultMapper(), clockPort,
                mock(QueueAuditService.class));
        service.callNext(new CallNextQueueItemCommand(queue.getId()));

        assertEquals(QueueItemStatus.IN_PROGRESS, item.getStatus());
        assertEquals(VisitStatus.IN_PROGRESS, visit.getStatus());
        verify(queueItemRepository).save(item);
        verify(visitRepository).save(visit);
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
        MedicalRecord lockedRecord = mock(MedicalRecord.class);
        when(queueItemRepository.findByIdForUpdate(item.getId())).thenReturn(Optional.of(item));
        when(queueRepository.findById(queue.getId())).thenReturn(Optional.of(queue));
        when(visitRepository.findByIdForUpdate(visit.getId())).thenReturn(Optional.of(visit));
        when(medicalRecordRepository.findByVisitId(visit.getId())).thenReturn(Optional.of(lockedRecord));
        when(lockedRecord.isLocked()).thenReturn(true);
        when(currentUserPort.hasRole("ADMIN")).thenReturn(true);
        when(clockPort.now()).thenReturn(now.plusSeconds(60));

        CompleteQueueItemService service = new CompleteQueueItemService(queueItemRepository, queueRepository,
                visitRepository, appointmentRepository, medicalRecordRepository, new QueueOperationAuthorization(currentUserPort),
                new QueueItemResultMapper(), clockPort, mock(QueueAuditService.class));
        service.complete(new CompleteQueueItemCommand(item.getId()));

        assertEquals(QueueItemStatus.COMPLETED, item.getStatus());
        assertEquals(VisitStatus.COMPLETED, visit.getStatus());
        verify(queueItemRepository).save(item);
        verify(visitRepository).save(visit);
    }
}
