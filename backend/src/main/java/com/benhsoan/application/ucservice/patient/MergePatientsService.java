package com.benhsoan.application.ucservice.patient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientChangeLog;
import com.benhsoan.domain.patient.enums.PatientChangeAction;
import com.benhsoan.domain.patient.exception.CannotMergeSamePatientException;
import com.benhsoan.domain.patient.exception.PatientAlreadyMergedException;
import com.benhsoan.domain.patient.exception.PatientIdentityConflictException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.port.dto.command.patient.MergePatientsCommand;
import com.benhsoan.port.dto.result.patient.MergePatientsResult;
import com.benhsoan.port.inbound.patient.MergePatientsUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserSessionRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChangeLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientMergeDataPort;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MergePatientsService implements MergePatientsUseCase {

    private final PatientRepository patientRepository;
    private final PatientMergeDataPort patientMergeDataPort;
    private final AuditLogRepository auditLogRepository;
    private final PatientChangeLogRepository patientChangeLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ObjectMapper objectMapper;
    private final UserSessionRepository userSessionRepository;

    @Override
    public MergePatientsResult merge(MergePatientsCommand command) {
        UUID sourceId = Objects.requireNonNull(command.sourcePatientId(), "sourcePatientId must not be null");
        UUID targetId = Objects.requireNonNull(command.targetPatientId(), "targetPatientId must not be null");

        if (sourceId.equals(targetId)) {
            throw new CannotMergeSamePatientException();
        }

        // Lock bi-directional rows in ordered UUID sequence to prevent database deadlocks
        UUID firstLockId = sourceId.compareTo(targetId) < 0 ? sourceId : targetId;
        UUID secondLockId = sourceId.compareTo(targetId) < 0 ? targetId : sourceId;
        Patient firstPatient = patientRepository.findByIdForUpdate(firstLockId)
                .orElseThrow(() -> new PatientNotFoundException(firstLockId));
        Patient secondPatient = patientRepository.findByIdForUpdate(secondLockId)
                .orElseThrow(() -> new PatientNotFoundException(secondLockId));

        Patient sourcePatient = sourceId.equals(firstLockId) ? firstPatient : secondPatient;
        Patient targetPatient = targetId.equals(firstLockId) ? firstPatient : secondPatient;

        if (sourcePatient.isMerged()) {
            throw new PatientAlreadyMergedException(sourceId, sourcePatient.getMergedIntoPatientId());
        }
        if (targetPatient.isMerged()) {
            throw new PatientAlreadyMergedException(targetId, targetPatient.getMergedIntoPatientId());
        }

        // QTN-33: Validate identity conflict
        validateIdentityConsistency(sourcePatient, targetPatient);

        // Transfer all associated clinical and billing data
        int transferredVisitsCount = patientMergeDataPort.transferAllPatientData(sourceId, targetId);

        UUID currentUserId = currentUserPort.getCurrentUserId();
        Instant now = Instant.now();

        // If source had user portal account, transfer or clean up to prevent duplicate user_id
        if (sourcePatient.getUserId() != null) {
            UUID sourceUserId = sourcePatient.getUserId();
            if (targetPatient.getUserId() == null) {
                targetPatient.linkUser(sourceUserId);
                patientRepository.save(targetPatient);
            } else {
                userSessionRepository.revokeByUserId(sourceUserId, now);
            }
            sourcePatient.unlinkUser();
        }

        // Mark source as merged
        sourcePatient.markAsMerged(targetId, currentUserId, command.reason(), now);
        patientRepository.save(sourcePatient);

        // Record PatientChangeLogs for both profiles
        Map<String, Object> sourceDetailMap = new LinkedHashMap<>();
        sourceDetailMap.put("action", "MERGED_INTO");
        sourceDetailMap.put("targetPatientId", targetPatient.getId().toString());
        sourceDetailMap.put("targetPatientCode", targetPatient.getPatientCode());
        sourceDetailMap.put("reason", command.reason());
        patientChangeLogRepository.save(PatientChangeLog.create(
                sourcePatient.getId(), currentUserId, PatientChangeAction.MERGE, toJson(sourceDetailMap)));

        Map<String, Object> targetDetailMap = new LinkedHashMap<>();
        targetDetailMap.put("action", "MERGED_FROM");
        targetDetailMap.put("sourcePatientId", sourcePatient.getId().toString());
        targetDetailMap.put("sourcePatientCode", sourcePatient.getPatientCode());
        targetDetailMap.put("transferredVisitsCount", transferredVisitsCount);
        patientChangeLogRepository.save(PatientChangeLog.create(
                targetPatient.getId(), currentUserId, PatientChangeAction.MERGE, toJson(targetDetailMap)));

        // System Audit Log (TC-05: Record source, target, operator, timestamp)
        Map<String, Object> auditDetailMap = new LinkedHashMap<>();
        auditDetailMap.put("sourcePatientId", sourcePatient.getId().toString());
        auditDetailMap.put("sourcePatientCode", sourcePatient.getPatientCode());
        auditDetailMap.put("targetPatientId", targetPatient.getId().toString());
        auditDetailMap.put("targetPatientCode", targetPatient.getPatientCode());
        auditDetailMap.put("operatorId", currentUserId.toString());
        auditDetailMap.put("reason", command.reason());
        auditDetailMap.put("transferredVisitsCount", transferredVisitsCount);
        auditDetailMap.put("mergedAt", now.toString());

        auditLogRepository.save(AuditLog.create(
                currentUserId, ActionType.MERGE, ResourceType.PATIENT, targetId, toJson(auditDetailMap), null, now));

        return MergePatientsResult.builder()
                .sourcePatientId(sourcePatient.getId())
                .sourcePatientCode(sourcePatient.getPatientCode())
                .targetPatientId(targetPatient.getId())
                .targetPatientCode(targetPatient.getPatientCode())
                .transferredVisitsCount(transferredVisitsCount)
                .mergedBy(currentUserId)
                .reason(command.reason())
                .mergedAt(now)
                .build();
    }

    private void validateIdentityConsistency(Patient source, Patient target) {
        if (hasText(source.getIdentityNumber()) && hasText(target.getIdentityNumber())
                && !source.getIdentityNumber().trim().equalsIgnoreCase(target.getIdentityNumber().trim())) {
            throw new PatientIdentityConflictException(
                    "Hai hồ sơ có số định danh cá nhân (CCCD/CMND) khác nhau: "
                            + source.getIdentityNumber() + " và " + target.getIdentityNumber() + " (QTN-33).");
        }

        boolean demographicMismatch = (source.getGender() != null && target.getGender() != null && source.getGender() != target.getGender())
                || (source.getDateOfBirth() != null && target.getDateOfBirth() != null && !source.getDateOfBirth().equals(target.getDateOfBirth()));

        if (demographicMismatch) {
            boolean sourceHasFinalized = patientMergeDataPort.hasFinalizedMedicalRecords(source.getId());
            boolean targetHasFinalized = patientMergeDataPort.hasFinalizedMedicalRecords(target.getId());
            if (sourceHasFinalized && targetHasFinalized) {
                throw new PatientIdentityConflictException(
                        "Hai hồ sơ mâu thuẫn về thông tin nhân khẩu (giới tính/ngày sinh) trong khi cả hai đều đã có bệnh án ký khóa chuyên môn (QTN-33).");
            }
        }
    }

    private boolean hasText(String str) {
        return str != null && !str.isBlank();
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize audit detail to JSON", e);
        }
    }
}
