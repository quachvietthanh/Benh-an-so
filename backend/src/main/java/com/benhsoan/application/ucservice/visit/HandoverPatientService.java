package com.benhsoan.application.ucservice.visit;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.medicalrecord.MedicalRecordAccessAuditService;
import com.benhsoan.application.ucservice.medicalrecord.MedicalRecordAuthorizationAuditService;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.constant.RoleConstants;
import com.benhsoan.domain.auth.exception.UserNotFoundException;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAlreadyLockedException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.VisitHandover;
import com.benhsoan.domain.visit.exception.VisitEncounterAccessDeniedException;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.visit.HandoverPatientCommand;
import com.benhsoan.port.dto.result.VisitHandoverResult;
import com.benhsoan.port.inbound.visit.HandoverPatientUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitHandoverRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class HandoverPatientService implements HandoverPatientUseCase {

    private final CurrentUserPort currentUserPort;
    private final VisitRepository visitRepository;
    private final VisitHandoverRepository visitHandoverRepository;
    private final UserRepository userRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordAccessAuditService medicalRecordAccessAuditService;
    private final MedicalRecordAuthorizationAuditService medicalRecordAuthorizationAuditService;
    private final AuditLogRepository auditLogRepository;
    private final ClockPort clockPort;

    @Override
    public VisitHandoverResult handover(UUID visitId, HandoverPatientCommand command) {
        if (visitId == null) {
            throw new ValidationException("Visit ID is required");
        }
        if (command == null) {
            throw new ValidationException("Handover command is required");
        }
        if (command.targetDoctorId() == null) {
            throw new ValidationException("Target doctor ID is required");
        }
        if (command.reason() == null || command.reason().isBlank()) {
            throw new ValidationException("Handover reason is required");
        }

        UUID actorId = currentUserPort.getCurrentUserId();
        boolean isAdmin = currentUserPort.hasRole("ADMIN");
        boolean isDoctor = currentUserPort.hasRole("DOCTOR");
        boolean hasHandoverPerm = currentUserPort.hasPermission("MEDICAL_RECORD_HANDOVER");

        if (!isAdmin && !(isDoctor || hasHandoverPerm)) {
            medicalRecordAuthorizationAuditService.recordHandoverAccessDenied(
                    actorId,
                    visitId,
                    "Actor " + actorId + " has no handover permission or doctor role"
            );
            throw new VisitEncounterAccessDeniedException();
        }

        Visit visit = visitRepository.findByIdForUpdate(visitId)
                .orElseThrow(() -> new VisitNotFoundException(visitId));

        if (!isAdmin && !visit.getDoctorId().equals(actorId)) {
            medicalRecordAuthorizationAuditService.recordHandoverAccessDenied(
                    actorId,
                    visit.getId(),
                    "Doctor " + actorId + " is not assigned doctor " + visit.getDoctorId() + " for visit " + visit.getId()
            );
            throw new VisitEncounterAccessDeniedException();
        }

        if (visit.isCompleted() || visit.isCancelled()) {
            throw new ValidationException("Cannot handover a completed or cancelled visit");
        }

        if (command.targetDoctorId().equals(visit.getDoctorId())) {
            throw new ValidationException("Cannot handover to the current doctor in charge");
        }

        User targetDoctor = userRepository.findById(command.targetDoctorId())
                .orElseThrow(() -> new UserNotFoundException(command.targetDoctorId().toString()));

        if (!targetDoctor.isActive() || !RoleConstants.DOCTOR.equals(targetDoctor.getRoleId())) {
            throw new ValidationException("Target user must be an active doctor");
        }

        Optional<MedicalRecord> recordOpt = medicalRecordRepository.findByVisitId(visit.getId());
        if (recordOpt.isPresent() && recordOpt.get().isContentLocked()) {
            throw new MedicalRecordAlreadyLockedException();
        }

        Instant now = clockPort.now();
        UUID fromDoctorId = visit.getDoctorId();
        String reason = command.reason().trim();

        visit.handover(command.targetDoctorId(), reason, now);
        visitRepository.save(visit);

        VisitHandover handover = VisitHandover.create(
                visit.getId(),
                fromDoctorId,
                command.targetDoctorId(),
                reason,
                actorId,
                now
        );
        VisitHandover savedHandover = visitHandoverRepository.save(handover);

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.UPDATE,
                ResourceType.VISIT,
                visit.getId(),
                "Patient handed over from doctor " + fromDoctorId + " to doctor " + command.targetDoctorId() + ". Reason: " + reason,
                null,
                now
        ));

        if (recordOpt.isPresent()) {
            medicalRecordAccessAuditService.recordRecordAccess(
                    visit.getPatientId(),
                    visit.getId(),
                    recordOpt.get().getId(),
                    actorId,
                    MedicalRecordAccessAction.HANDOVER,
                    "Patient handed over to doctor: " + targetDoctor.getFullName() + ". Reason: " + reason,
                    now
            );
        }

        String fromDoctorName = userRepository.findById(fromDoctorId)
                .map(User::getFullName)
                .orElse("Unknown");

        return new VisitHandoverResult(
                savedHandover.getId(),
                savedHandover.getVisitId(),
                savedHandover.getFromDoctorId(),
                fromDoctorName,
                savedHandover.getToDoctorId(),
                targetDoctor.getFullName(),
                savedHandover.getReason(),
                savedHandover.getHandedOverAt()
        );
    }
}
