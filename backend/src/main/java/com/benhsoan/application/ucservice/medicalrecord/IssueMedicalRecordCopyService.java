package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.UserNotFoundException;
import com.benhsoan.domain.clinic.ClinicConfiguration;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordMissingAuthorizationException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotSignedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordUnauthorizedRecipientException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.medicalrecord.IssueMedicalRecordCopyCommand;
import com.benhsoan.port.dto.command.medicalrecord.MedicalRecordCopyRecipientType;
import com.benhsoan.port.dto.result.MedicalRecordCopyDocument;
import com.benhsoan.port.dto.result.MedicalRecordCopyResult;
import com.benhsoan.port.inbound.medicalrecord.IssueMedicalRecordCopyUseCase;
import com.benhsoan.port.outbound.pdf.MedicalRecordCopyPdfRenderer;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.clinic.ClinicConfigurationRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class IssueMedicalRecordCopyService implements IssueMedicalRecordCopyUseCase {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;
    private final VisitRepository visitRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final ClinicConfigurationRepository clinicConfigurationRepository;
    private final MedicalRecordCopyPdfRenderer pdfRenderer;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final AuditLogRepository auditLogRepository;
    private final MedicalRecordCopyAuditWriter copyAuditWriter;
    private final MedicalRecordAccessAuditService accessAuditService;
    private final ObjectMapper objectMapper;

    @Override
    public MedicalRecordCopyResult issue(IssueMedicalRecordCopyCommand command) {
        MedicalRecord record = medicalRecordRepository.findById(command.medicalRecordId())
                .orElseThrow(() -> new MedicalRecordNotFoundException(command.medicalRecordId()));
        authorizeCopyRole();
        ensureSigned(record);

        Visit visit = visitRepository.findById(record.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(record.getVisitId()));
        Patient patient = patientRepository.findById(visit.getPatientId())
                .orElseThrow(() -> new PatientNotFoundException(visit.getPatientId()));
        UUID actorId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();
        if (command.recipientType() == MedicalRecordCopyRecipientType.PATIENT) {
            verifyPatientIdentity(patient, command, record.getId(), actorId, now);
        } else {
            verifyAuthorizedRepresentative(command, record.getId(), actorId, now);
        }
        User doctor = userRepository.findById(visit.getDoctorId())
                .orElseThrow(() -> new UserNotFoundException(visit.getDoctorId().toString()));
        ClinicConfiguration clinic = clinicConfigurationRepository.find()
                .orElseThrow(() -> new ValidationException("Clinic configuration is required for issuing a copy."));
        List<MedicalRecordDiagnosis> diagnoses = medicalRecordDiagnosisRepository.findByMedicalRecordId(record.getId());

        byte[] content = pdfRenderer.render(toDocument(record, visit, patient, doctor, clinic, diagnoses));
        recordCopyAudits(record, visit, patient, command);

        return new MedicalRecordCopyResult(
                "medical-record-copy-" + patient.getPatientCode() + ".pdf",
                PDF_CONTENT_TYPE,
                content
        );
    }

    private void authorizeCopyRole() {
        if (!currentUserPort.hasRole("ADMIN") && !currentUserPort.hasRole("MANAGER")) {
            throw new AccessDeniedException("Only managers and administrators can issue medical record copies.");
        }
    }

    private void ensureSigned(MedicalRecord record) {
        if (!record.isContentLocked()) {
            throw new MedicalRecordNotSignedException(record.getId());
        }
    }

    private void verifyPatientIdentity(Patient patient, IssueMedicalRecordCopyCommand command,
            UUID medicalRecordId, UUID actorId, Instant now) {
        if (!matchesPatientIdentity(patient, command.recipientIdentityNumber())
                || !matchesPatientName(patient, command.recipientName())) {
            writeDenialAudit(medicalRecordId, actorId, command,
                    "Recipient identity does not match the medical record patient.", now);
            throw new MedicalRecordUnauthorizedRecipientException(medicalRecordId, command.recipientName());
        }
    }

    private void verifyAuthorizedRepresentative(IssueMedicalRecordCopyCommand command,
            UUID medicalRecordId, UUID actorId, Instant now) {
        if (isBlank(command.authorizationDocumentNumber()) || isBlank(command.requestReason())) {
            writeDenialAudit(medicalRecordId, actorId, command,
                    "Authorization document is required for an authorized representative.", now);
            throw new MedicalRecordMissingAuthorizationException(medicalRecordId);
        }
    }

    private boolean matchesPatientIdentity(Patient patient, String recipientIdentityNumber) {
        return patient.getIdentityNumber() != null
                && patient.getIdentityNumber().equalsIgnoreCase(trimToEmpty(recipientIdentityNumber));
    }

    private boolean matchesPatientName(Patient patient, String recipientName) {
        return patient.getFullName() != null
                && patient.getFullName().trim().equalsIgnoreCase(trimToEmpty(recipientName));
    }

    private void writeDenialAudit(UUID medicalRecordId, UUID actorId, IssueMedicalRecordCopyCommand command,
            String reason, Instant now) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("issuedBy", actorId.toString());
        detail.put("recipientType", command.recipientType().name());
        detail.put("recipientName", command.recipientName());
        detail.put("recipientIdentityNumber", command.recipientIdentityNumber());
        detail.put("authorizationDocumentNumber", command.authorizationDocumentNumber());
        detail.put("requestReason", command.requestReason());
        detail.put("denialReason", reason);
        detail.put("deniedAt", now.toString());
        copyAuditWriter.writeDenied(actorId, medicalRecordId, toJson(detail), now);
    }

    private String toJson(Map<String, Object> detail) {
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not serialize medical record copy audit detail.", ex);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private MedicalRecordCopyDocument toDocument(
            MedicalRecord record,
            Visit visit,
            Patient patient,
            User doctor,
            ClinicConfiguration clinic,
            List<MedicalRecordDiagnosis> diagnoses
    ) {
        List<MedicalRecordCopyDocument.Diagnosis> diagnosisItems = diagnoses.stream()
                .map(d -> new MedicalRecordCopyDocument.Diagnosis(d.getDiagnosisCode(), d.getDiagnosisName()))
                .toList();
        return new MedicalRecordCopyDocument(
                clinic.getClinicName(), clinic.getAddress(), clinic.getPhone(),
                patient.getPatientCode(), patient.getFullName(),
                patient.getDateOfBirth() == null ? null : patient.getDateOfBirth().toString(),
                patient.getGender() == null ? null : patient.getGender().name(),
                visit.getVisitCode(), visit.getVisitAt(), doctor.getFullName(),
                record.getChiefComplaint(), record.getSymptoms(), record.getMedicalHistory(),
                record.getPhysicalExamination(), record.getClinicalProgress(), record.getTreatmentPlan(),
                record.getDoctorInstructions(), record.getConclusion(),
                diagnosisItems
        );
    }

    private void recordCopyAudits(
            MedicalRecord record,
            Visit visit,
            Patient patient,
            IssueMedicalRecordCopyCommand command
    ) {
        UUID issuedBy = currentUserPort.getCurrentUserId();
        Instant issuedAt = clockPort.now();
        auditLogRepository.save(AuditLog.create(
                issuedBy, ActionType.EXPORT, ResourceType.MEDICAL_RECORD,
                record.getId(), toDetailJson(issuedBy, command, issuedAt), null, issuedAt
        ));
        accessAuditService.recordRecordAccess(
                patient.getId(), visit.getId(), record.getId(), issuedBy,
                MedicalRecordAccessAction.EXPORT, "Medical record copy issued", issuedAt
        );
    }

    private String toDetailJson(UUID issuedBy, IssueMedicalRecordCopyCommand command, Instant issuedAt) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("issuedBy", issuedBy.toString());
        detail.put("recipientType", command.recipientType().name());
        detail.put("recipientName", command.recipientName());
        detail.put("recipientIdentityNumber", command.recipientIdentityNumber());
        detail.put("authorizationDocumentNumber", command.authorizationDocumentNumber());
        detail.put("requestReason", command.requestReason());
        detail.put("issuedAt", issuedAt.toString());
        return toJson(detail);
    }
}

