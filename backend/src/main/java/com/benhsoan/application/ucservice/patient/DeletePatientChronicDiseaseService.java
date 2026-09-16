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
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.PatientChronicDisease;
import com.benhsoan.domain.patient.exception.PatientChronicDiseaseNotFoundException;
import com.benhsoan.domain.patient.exception.PatientInactiveException;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.patient.DeletePatientChronicDiseaseCommand;
import com.benhsoan.port.inbound.patient.DeletePatientChronicDiseaseUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.patient.PatientChronicDiseaseRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeletePatientChronicDiseaseService implements DeletePatientChronicDiseaseUseCase {

    private final PatientRepository patientRepository;
    private final PatientChronicDiseaseRepository patientChronicDiseaseRepository;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final ObjectMapper objectMapper;

    @Override
    public void deleteChronicDisease(DeletePatientChronicDiseaseCommand command) {
        if (command == null || command.chronicDiseaseId() == null || command.patientId() == null) {
            throw new ValidationException("Chronic disease ID and Patient ID are required.");
        }

        Patient patient = patientRepository.findById(command.patientId())
                .orElseThrow(() -> new PatientNotFoundException(command.patientId()));
        if (!patient.isActive()) {
            throw new PatientInactiveException();
        }

        PatientChronicDisease chronicDisease = patientChronicDiseaseRepository.findById(command.chronicDiseaseId())
                .orElseThrow(() -> new PatientChronicDiseaseNotFoundException(command.chronicDiseaseId()));
        if (!chronicDisease.getPatientId().equals(command.patientId()) || !chronicDisease.isActive()) {
            throw new PatientChronicDiseaseNotFoundException(command.chronicDiseaseId());
        }

        UUID currentUserId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();

        chronicDisease.deactivate(currentUserId, now);
        patientChronicDiseaseRepository.save(chronicDisease);

        auditLogRepository.save(AuditLog.create(
                currentUserId,
                ActionType.DELETE,
                ResourceType.PATIENT_CHRONIC_DISEASE,
                chronicDisease.getId(),
                buildDeleteAuditDetail(command.reason()),
                null,
                now
        ));
    }

    private String buildDeleteAuditDetail(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("reason", reason);
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize delete audit detail.", exception);
        }
    }
}
