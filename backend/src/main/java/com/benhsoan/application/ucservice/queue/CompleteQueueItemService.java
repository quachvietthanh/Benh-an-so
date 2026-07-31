package com.benhsoan.application.ucservice.queue;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.domain.queue.exception.CheckInConflictException;
import com.benhsoan.domain.queue.exception.QueueItemNotFoundException;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.queue.CompleteQueueItemCommand;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.inbound.queue.CompleteQueueItemUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.crudRepository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.crudRepository.queue.MedicalQueueRepository;
import com.benhsoan.port.outbound.repository.crudRepository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.repository.crudRepository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CompleteQueueItemService implements CompleteQueueItemUseCase {

    private final QueueItemRepository queueItemRepository;
    private final MedicalQueueRepository medicalQueueRepository;
    private final VisitRepository visitRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final QueueOperationAuthorization authorization;
    private final QueueItemResultMapper resultMapper;
    private final ClockPort clockPort;
    private final QueueAuditService queueAuditService;

    @Override
    public QueueItemResult complete(CompleteQueueItemCommand command) {
        QueueItem item = queueItemRepository.findByIdForUpdate(command.queueItemId())
                .orElseThrow(() -> new QueueItemNotFoundException(command.queueItemId()));
        var queue = medicalQueueRepository.findById(item.getMedicalQueueId())
                .orElseThrow(() -> new QueueItemNotFoundException(item.getMedicalQueueId()));
        authorization.requireCompletePermission(queue);
        var visit = visitRepository.findByIdForUpdate(item.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(item.getVisitId()));
        var medicalRecord = medicalRecordRepository.findByVisitId(visit.getId())
                .orElseThrow(() -> new CheckInConflictException("Visit must have a locked medical record before completion."));
        if (!medicalRecord.isLocked()) {
            throw new CheckInConflictException("Medical record must be locked before visit completion.");
        }

        var now = clockPort.now();
        item.complete(now);
        visit.complete(now);
        if (item.getAppointmentId() != null) {
            var appointment = appointmentRepository.findByIdForUpdate(item.getAppointmentId())
                    .orElseThrow(() -> new AppointmentNotFoundException(item.getAppointmentId()));
            appointment.complete(now);
            appointmentRepository.save(appointment);
        }
        queueItemRepository.save(item);
        visitRepository.save(visit);
        queueAuditService.record(com.benhsoan.domain.auditlog.enums.ActionType.UPDATE, item);
        return resultMapper.toResult(item);
    }
}
