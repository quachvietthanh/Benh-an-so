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
import com.benhsoan.domain.medicalrecord.DiagnosisCatalog;
import com.benhsoan.domain.medicalrecord.exception.DiagnosisCatalogNotFoundException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientChronicDisease;
import com.benhsoan.domain.patient.exception.PatientChronicDiseaseAlreadyExistsException;
import com.benhsoan.domain.patient.exception.PatientInactiveException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.patient.AddPatientChronicDiseaseCommand;
import com.benhsoan.port.dto.result.patient.PatientChronicDiseaseResult;
import com.benhsoan.port.inbound.patient.AddPatientChronicDiseaseUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.DiagnosisCatalogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChronicDiseaseRepository;
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
public class AddPatientChronicDiseaseService implements AddPatientChronicDiseaseUseCase {

    private final PatientRepository patientRepository;
    private final PatientChronicDiseaseRepository patientChronicDiseaseRepository;
    private final DiagnosisCatalogRepository diagnosisCatalogRepository;
    private final VisitRepository visitRepository;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final PatientChronicDiseaseResultMapper resultMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PatientChronicDiseaseResult addChronicDisease(AddPatientChronicDiseaseCommand command) {
        if (command == null || command.patientId() == null) {
            throw new ValidationException("Patient ID is required.");
        }
        if (command.diagnosisCatalogId() == null) {
            throw new ValidationException("Mã bệnh (chẩn đoán) không được để trống.");
        }

        Patient patient = patientRepository.findById(command.patientId())
                .orElseThrow(() -> new PatientNotFoundException(command.patientId()));
        if (!patient.isActive()) {
            throw new PatientInactiveException();
        }

        DiagnosisCatalog catalog = diagnosisCatalogRepository.findById(command.diagnosisCatalogId())
                .orElseThrow(() -> new DiagnosisCatalogNotFoundException(command.diagnosisCatalogId()));
        if (!catalog.isActive()) {
            throw new ValidationException("Mã bệnh (chẩn đoán) không còn hiệu lực.");
        }

        if (command.visitId() != null) {
            validateVisit(command.patientId(), command.visitId());
        }

        if (patientChronicDiseaseRepository.existsByPatientIdAndDiagnosisCatalogIdAndActiveTrue(
                command.patientId(), command.diagnosisCatalogId())) {
            throw new PatientChronicDiseaseAlreadyExistsException();
        }

        UUID currentUserId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();

        PatientChronicDisease chronicDisease = PatientChronicDisease.create(
                command.patientId(),
                command.diagnosisCatalogId(),
                command.yearDetected(),
                command.notes(),
                currentUserId,
                now
        );

        PatientChronicDisease saved = patientChronicDiseaseRepository.save(chronicDisease);

        auditLogRepository.save(AuditLog.create(
                currentUserId,
                ActionType.CREATE,
                ResourceType.PATIENT_CHRONIC_DISEASE,
                saved.getId(),
                buildAuditDetail(saved, command.visitId()),
                null,
                now
        ));

        return resultMapper.toResult(saved, catalog.getCode(), catalog.getName());
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

    private String buildAuditDetail(PatientChronicDisease chronicDisease, UUID visitId) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("chronicDiseaseId", chronicDisease.getId().toString());
        detail.put("patientId", chronicDisease.getPatientId().toString());
        detail.put("diagnosisCatalogId", chronicDisease.getDiagnosisCatalogId().toString());
        if (visitId != null) {
            detail.put("visitId", visitId.toString());
        }
        if (chronicDisease.getYearDetected() != null) {
            detail.put("yearDetected", chronicDisease.getYearDetected());
        }
        if (chronicDisease.getNotes() != null) {
            detail.put("notes", chronicDisease.getNotes());
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize chronic disease audit detail.", exception);
        }
    }
}
