package com.benhsoan.application.ucservice.queue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.queue.DoctorRoomAssignment;
import com.benhsoan.domain.queue.MedicalQueue;
import com.benhsoan.domain.queue.QueueItem;
import com.benhsoan.domain.queue.enums.MedicalQueueStatus;
import com.benhsoan.domain.queue.enums.QueueItemSourceType;
import com.benhsoan.domain.queue.enums.QueueItemStatus;
import com.benhsoan.domain.queue.exception.CheckInConflictException;
import com.benhsoan.domain.queue.exception.DoctorNotAssignedToRoomException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.port.dto.result.QueueCheckInResult;
import com.benhsoan.port.outbound.generator.VisitCodeGenerator;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.queue.DoctorRoomAssignmentRepository;
import com.benhsoan.port.outbound.repository.queue.MedicalQueueRepository;
import com.benhsoan.port.outbound.repository.queue.QueueItemRepository;
import com.benhsoan.port.outbound.repository.queue.RoomRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class QueueCheckInCoordinator {

    private static final ZoneId CLINIC_ZONE_ID = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<VisitStatus> ACTIVE_VISIT_STATUSES = List.of(
            VisitStatus.WAITING, VisitStatus.IN_PROGRESS, VisitStatus.WAITING_FOR_RESULT);
    private static final List<QueueItemStatus> ACTIVE_QUEUE_ITEM_STATUSES = List.of(
            QueueItemStatus.WAITING, QueueItemStatus.IN_PROGRESS, QueueItemStatus.WAITING_FOR_RESULT);

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final DoctorRoomAssignmentRepository doctorRoomAssignmentRepository;
    private final RoomRepository roomRepository;
    private final MedicalQueueRepository medicalQueueRepository;
    private final QueueItemRepository queueItemRepository;
    private final VisitRepository visitRepository;
    private final VisitCodeGenerator visitCodeGenerator;
    private final AuditLogRepository auditLogRepository;

    QueueCheckInResult checkIn(UUID patientId, UUID doctorId, UUID appointmentId, QueueItemSourceType sourceType,
            String reason, String note, UUID actorId, Instant checkedInAt) {
        var patient = patientRepository.findByIdForUpdate(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
        if (!patient.isActive()) {
            throw new CheckInConflictException("Inactive patients cannot be checked in.");
        }

        LocalDate queueDate = checkedInAt.atZone(CLINIC_ZONE_ID).toLocalDate();
        ensurePatientHasNoActiveCareFlow(patientId, queueDate);

        var doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new CheckInConflictException("Assigned doctor does not exist."));
        if (!doctor.isActive()) {
            throw new CheckInConflictException("Inactive doctors cannot receive queue items.");
        }

        DoctorRoomAssignment assignment = doctorRoomAssignmentRepository.findByDoctorIdForUpdate(doctorId)
                .orElseThrow(() -> new DoctorNotAssignedToRoomException(doctorId));
        roomRepository.findActiveById(assignment.getRoomId())
                .orElseThrow(() -> new DoctorNotAssignedToRoomException(doctorId));

        MedicalQueue medicalQueue = getOrCreateOpenQueue(doctorId, assignment.getRoomId(), queueDate, checkedInAt);
        Visit visit = Visit.create(visitCodeGenerator.generate(), patientId, doctorId, appointmentId, null,
                sourceType == QueueItemSourceType.APPOINTMENT ? VisitType.APPOINTMENT : VisitType.WALK_IN,
                checkedInAt, reason, note, actorId, checkedInAt);
        Visit savedVisit = visitRepository.save(visit);

        int queueNumber = queueItemRepository.findMaxQueueNumber(medicalQueue.getId()) + 1;
        QueueItem queueItem = QueueItem.create(medicalQueue.getId(), patientId, appointmentId, savedVisit.getId(),
                sourceType, queueNumber, queueDate, actorId, checkedInAt);
        QueueItem savedQueueItem = queueItemRepository.save(queueItem);

        savedVisit.assignQueueItem(savedQueueItem.getId(), checkedInAt);
        Visit linkedVisit = visitRepository.save(savedVisit);

        auditLogRepository.save(AuditLog.create(actorId, ActionType.CREATE, ResourceType.VISIT, linkedVisit.getId(),
                "{\"queueItemId\":\"%s\",\"sourceType\":\"%s\",\"queueNumber\":%d}"
                        .formatted(savedQueueItem.getId(), sourceType, queueNumber), null));

        return new QueueCheckInResult(savedQueueItem.getId(), medicalQueue.getId(), linkedVisit.getId(),
                linkedVisit.getVisitCode(), appointmentId, patientId, doctorId, assignment.getRoomId(), queueNumber,
                queueDate, sourceType, savedQueueItem.getStatus(), linkedVisit.getStatus(), checkedInAt);
    }

    void requireAppointmentOnQueueDate(Instant appointmentStartTime, Instant checkedInAt) {
        LocalDate appointmentDate = appointmentStartTime.atZone(CLINIC_ZONE_ID).toLocalDate();
        LocalDate queueDate = checkedInAt.atZone(CLINIC_ZONE_ID).toLocalDate();
        if (!appointmentDate.equals(queueDate)) {
            throw new CheckInConflictException("Appointment can only be checked in on its scheduled date.");
        }
    }

    private void ensurePatientHasNoActiveCareFlow(UUID patientId, LocalDate queueDate) {
        ZonedDateTime queueDateStart = queueDate.atStartOfDay(CLINIC_ZONE_ID);
        Instant fromInclusive = queueDateStart.toInstant();
        Instant toExclusive = queueDateStart.plusDays(1).toInstant();

        if (visitRepository.existsByPatientIdAndStatusInAndVisitAtBetween(
                patientId,
                ACTIVE_VISIT_STATUSES,
                fromInclusive,
                toExclusive
        )
                || queueItemRepository.existsByPatientIdAndQueueDateAndStatusIn(patientId, queueDate,
                        ACTIVE_QUEUE_ITEM_STATUSES)) {
            throw new CheckInConflictException("Patient already has an active visit or queue item.");
        }
    }

    private MedicalQueue getOrCreateOpenQueue(UUID doctorId, UUID roomId, LocalDate queueDate, Instant createdAt) {
        MedicalQueue medicalQueue = medicalQueueRepository.findByDoctorIdAndQueueDateForUpdate(doctorId, queueDate)
                .orElseGet(() -> medicalQueueRepository.save(MedicalQueue.create(doctorId, roomId, queueDate, createdAt)));
        if (medicalQueue.getStatus() != MedicalQueueStatus.OPEN) {
            throw new CheckInConflictException("Doctor queue is closed for the selected date.");
        }
        return medicalQueue;
    }
}
