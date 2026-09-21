package com.benhsoan.application.ucservice.patient;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientChangeLog;
import com.benhsoan.domain.patient.enums.PatientChangeAction;
import com.benhsoan.domain.patient.enums.PregnancyStatus;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.PatientResult;
import com.benhsoan.port.inbound.patient.UpdatePatientPregnancyStatusUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChangeLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdatePatientPregnancyStatusService implements UpdatePatientPregnancyStatusUseCase {

    private final PatientRepository patientRepository;
    private final PatientResultMapper patientResultMapper;
    private final PatientChangeLogRepository patientChangeLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public PatientResult update(UUID patientId, PregnancyStatus pregnancyStatus) {
        if (patientId == null) {
            throw new ValidationException("Patient id is required.");
        }

        Instant now = clockPort.now();
        UUID actorId = currentUserPort.getCurrentUserId();

        Patient patient = patientRepository.findByIdForUpdate(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));

        PregnancyStatus oldStatus = patient.getPregnancyStatus();
        patient.changePregnancyStatus(pregnancyStatus);
        Patient saved = patientRepository.save(patient);

        String detail = "{\"field\":\"pregnancyStatus\",\"oldValue\":%s,\"newValue\":%s}".formatted(
                jsonValue(oldStatus), jsonValue(pregnancyStatus));

        patientChangeLogRepository.save(PatientChangeLog.restore(
                UUID.randomUUID(), saved.getId(), actorId, PatientChangeAction.UPDATE, detail, now));

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.UPDATE,
                ResourceType.PATIENT,
                saved.getId(),
                detail,
                null,
                now));

        return patientResultMapper.toResult(saved);
    }

    private static String jsonValue(PregnancyStatus status) {
        return status == null ? "null" : "\"" + status.name() + "\"";
    }
}
