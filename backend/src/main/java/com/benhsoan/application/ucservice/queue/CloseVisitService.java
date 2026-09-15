package com.benhsoan.application.ucservice.queue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.domain.appointment.exception.AppointmentNotFoundException;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAlreadyLockedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.exception.QueueItemNotFoundException;
import com.benhsoan.domain.queue.exception.QueueNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitCloseOutcome;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.queue.CloseVisitCommand;
import com.benhsoan.port.dto.result.QueueItemResult;
import com.benhsoan.port.inbound.queue.CloseVisitUseCase;
import com.benhsoan.port.outbound.repository.appointment.AppointmentRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.queue.MedicalQueueRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemQueryRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CloseVisitService implements CloseVisitUseCase {

    private final QueueItemRepository queueItemRepository;
    private final MedicalQueueRepository medicalQueueRepository;
    private final VisitRepository visitRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final QueueOperationAuthorization authorization;
    private final QueueItemQueryRepository queueItemQueryRepository;
    private final ClockPort clockPort;
    private final QueueAuditService queueAuditService;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;

    @Override
    public QueueItemResult close(CloseVisitCommand command) {
        if (command == null || command.queueItemId() == null) {
            throw new ValidationException("Queue item id is required.");
        }
        if (command.outcome() == null) {
            throw new ValidationException("Outcome is required.");
        }
        String reason = validateReason(command.reason());

        QueueItem item = queueItemRepository.findByIdForUpdate(command.queueItemId())
                .orElseThrow(() -> new QueueItemNotFoundException(command.queueItemId()));
        var queue = medicalQueueRepository.findByIdForUpdate(item.getMedicalQueueId())
                .orElseThrow(() -> new QueueNotFoundException(item.getMedicalQueueId()));
        authorization.requireClinicalUpdatePermission(queue);

        Visit visit = visitRepository.findByIdForUpdate(item.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(item.getVisitId()));

        var now = clockPort.now();

        UUID medicalRecordId = lockMedicalRecordIfPresent(visit);

        if (command.outcome() == VisitCloseOutcome.EARLY_ENDED) {
            visit.earlyEnd(reason, now);
        } else {
            visit.cancel(reason, now);
        }

        item.cancel(reason, now);

        if (item.getAppointmentId() != null) {
            Appointment appointment = appointmentRepository.findByIdForUpdate(item.getAppointmentId())
                    .orElseThrow(() -> new AppointmentNotFoundException(item.getAppointmentId()));
            appointment.cancel(reason);
            appointmentRepository.save(appointment);
        }

        cancelUndispensedPrescriptions(medicalRecordId, reason, now);

        queueItemRepository.save(item);
        visitRepository.save(visit);

        if (command.outcome() == VisitCloseOutcome.EARLY_ENDED) {
            queueAuditService.recordEarlyEnded(item, reason);
        } else {
            queueAuditService.recordCancelled(item, reason);
        }

        return queueItemQueryRepository.findDetailById(item.getId())
                .orElseThrow(() -> new QueueItemNotFoundException(item.getId()));
    }

    private UUID lockMedicalRecordIfPresent(Visit visit) {
        var existing = medicalRecordRepository.findByVisitId(visit.getId());
        if (existing.isEmpty()) {
            return null;
        }
        MedicalRecord record = medicalRecordRepository.findByIdForUpdate(existing.get().getId())
                .orElseThrow(() -> new MedicalRecordNotFoundException(existing.get().getId()));
        if (record.isContentLocked()) {
            throw new MedicalRecordAlreadyLockedException();
        }
        return record.getId();
    }

    private void cancelUndispensedPrescriptions(UUID medicalRecordId, String reason, Instant at) {
        if (medicalRecordId == null) {
            return;
        }
        List<Prescription> pending = prescriptionRepository.findByMedicalRecordIdAndStatusForUpdate(
                medicalRecordId, PrescriptionStatus.PENDING_DISPENSE);
        UUID actorId = currentUserPort.getCurrentUserId();
        for (Prescription prescription : pending) {
            prescription.cancel(reason, actorId, at);
            Prescription saved = prescriptionRepository.save(prescription);
            auditLogRepository.save(AuditLog.create(
                    actorId,
                    ActionType.CANCEL,
                    ResourceType.PRESCRIPTION,
                    saved.getId(),
                    "{\"prescriptionCode\":\"%s\",\"cancelReason\":\"%s\",\"cancelledAt\":\"%s\"}"
                            .formatted(saved.getPrescriptionCode(), reason, at.toString()),
                    null,
                    at
            ));
        }
    }

    private String validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Close reason is required.");
        }
        String trimmed = reason.trim();
        if (trimmed.length() > 500) {
            throw new ValidationException("Close reason must not exceed 500 characters.");
        }
        return trimmed;
    }
}
