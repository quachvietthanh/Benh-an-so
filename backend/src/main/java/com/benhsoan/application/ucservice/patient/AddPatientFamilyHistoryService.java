package com.benhsoan.application.ucservice.patient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogNotFoundException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientFamilyHistory;
import com.benhsoan.domain.patient.exception.PatientInactiveException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.patient.AddPatientFamilyHistoryCommand;
import com.benhsoan.port.dto.result.patient.PatientFamilyHistoryResult;
import com.benhsoan.port.inbound.patient.AddPatientFamilyHistoryUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientFamilyHistoryRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AddPatientFamilyHistoryService implements AddPatientFamilyHistoryUseCase {

    private final PatientRepository patientRepository;
    private final PatientFamilyHistoryRepository patientFamilyHistoryRepository;
    private final DiagnosisCatalogRepository diagnosisCatalogRepository;
    private final VisitRepository visitRepository;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final PatientFamilyHistoryResultMapper resultMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PatientFamilyHistoryResult addFamilyHistory(AddPatientFamilyHistoryCommand command) {
        if (command == null || command.patientId() == null) {
            throw new ValidationException("Patient ID is required.");
        }
        if (command.relationship() == null || command.relationship().isBlank()) {
            throw new ValidationException("Mối quan hệ gia đình không được để trống.");
        }
        if (command.diagnosisCatalogId() == null) {
            throw new ValidationException("Mã bệnh (chẩn đoán) không được để trống.");
        }

        Patient patient = patientRepository.findById(command.patientId())
                .orElseThrow(() -> new PatientNotFoundException(command.patientId()));
        if (!patient.isActive()) {
            throw new PatientInactiveException();
        }

        diagnosisCatalogRepository.findById(command.diagnosisCatalogId())
                .orElseThrow(() -> new DiagnosisCatalogNotFoundException(command.diagnosisCatalogId()));

        if (command.visitId() != null) {
            validateVisit(command.patientId(), command.visitId());
        }

        UUID currentUserId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();

        PatientFamilyHistory familyHistory = PatientFamilyHistory.create(
                command.patientId(),
                command.relationship(),
                command.diagnosisCatalogId(),
                command.notes(),
                currentUserId,
                now
        );

        PatientFamilyHistory saved = patientFamilyHistoryRepository.save(familyHistory);

        auditLogRepository.save(AuditLog.create(
                currentUserId,
                ActionType.CREATE,
                ResourceType.PATIENT_FAMILY_HISTORY,
                saved.getId(),
                buildAuditDetail(saved),
                null,
                now
        ));

        return resultMapper.toResult(saved);
    }

    private void validateVisit(UUID patientId, UUID visitId) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new VisitNotFoundException(visitId));
        if (!visit.getPatientId().equals(patientId)) {
            throw new ValidationException("Lượt khám không thuộc về bệnh nhân này.");
        }
        if (!visit.isActive()) {
            throw new ValidationException("Lượt khám đã kết thúc hoặc không còn hiệu lực.");
        }
    }

    private String buildAuditDetail(PatientFamilyHistory familyHistory) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("familyHistoryId", familyHistory.getId().toString());
        detail.put("patientId", familyHistory.getPatientId().toString());
        detail.put("relationship", familyHistory.getRelationship());
        detail.put("diagnosisCatalogId", familyHistory.getDiagnosisCatalogId().toString());
        if (familyHistory.getNotes() != null) {
            detail.put("notes", familyHistory.getNotes());
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize family history audit detail.", exception);
        }
    }
}
