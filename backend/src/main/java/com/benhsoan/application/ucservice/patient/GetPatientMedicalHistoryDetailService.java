package com.benhsoan.application.ucservice.patient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.application.ucservice.patient.PatientAccessGuard;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.result.patient.PatientMedicalHistoryDetailResult;
import com.benhsoan.port.dto.result.patient.PatientMedicalHistoryDetailResult.DiagnosisItem;
import com.benhsoan.port.dto.result.patient.PatientMedicalHistoryDetailResult.PrescriptionItemView;
import com.benhsoan.port.inbound.patient.GetPatientMedicalHistoryDetailUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionItemRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-005 CV-02: returns the detailed, finalized medical record + prescription for a
 * single visit, scoped strictly to the authenticated patient (QTN-23) with READ audit (TC-04).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GetPatientMedicalHistoryDetailService implements GetPatientMedicalHistoryDetailUseCase {

    private static final String ONLINE_PORTAL = "ONLINE_PORTAL";

    private final VisitRepository visitRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordDiagnosisRepository diagnosisRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final UserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;
    private final PatientAccessGuard patientAccessGuard;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final ObjectMapper objectMapper;

    @Override
    public PatientMedicalHistoryDetailResult getMedicalHistoryDetail(UUID visitId) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new VisitNotFoundException(visitId));

        // TC-03 / QTN-23: reject cross-patient access (403) and record ACCESS_DENIED audit.
        patientAccessGuard.requirePatientOwnership(
                visit.getPatientId(),
                ResourceType.MEDICAL_RECORD,
                visitId);

        // CV-01 / TC-03: only finalized (SIGNED/LOCKED/ARCHIVED) records are visible.
        MedicalRecord record = medicalRecordRepository.findByVisitId(visitId)
                .filter(MedicalRecord::isContentLocked)
                .orElseThrow(() -> new MedicalRecordNotFoundException(visitId));

        List<DiagnosisItem> diagnoses = diagnosisRepository.findByMedicalRecordId(record.getId()).stream()
                .map(this::toDiagnosis)
                .toList();

        List<PrescriptionItemView> items = prescriptionRepository.findByMedicalRecordId(record.getId()).stream()
                .flatMap(prescription -> prescriptionItemRepository.findByPrescriptionId(prescription.getId()).stream())
                .map(item -> new PrescriptionItemView(
                        item.getMedicineName(),
                        item.getQuantity(),
                        item.getDosage(),
                        item.getInstructions()))
                .toList();

        Instant viewedAt = clockPort.now();
        auditLogRepository.save(AuditLog.create(
                currentUserPort.getCurrentUserId(),
                ActionType.READ,
                ResourceType.MEDICAL_RECORD,
                record.getId(),
                auditDetail(visit, viewedAt),
                null,
                viewedAt
        ));

        return new PatientMedicalHistoryDetailResult(
                visit.getId(),
                visit.getVisitAt(),
                userRepository.findById(visit.getDoctorId()).map(User::getFullName).orElse(null),
                specialtyRepository.findById(visit.getSpecialtyId()).map(Specialty::getName).orElse(null),
                diagnoses,
                items,
                record.getDoctorInstructions()
        );
    }

    private DiagnosisItem toDiagnosis(MedicalRecordDiagnosis diagnosis) {
        return new DiagnosisItem(diagnosis.getDiagnosisCode(), diagnosis.getDiagnosisName());
    }

    private String auditDetail(Visit visit, Instant viewedAt) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("channel", ONLINE_PORTAL);
        detail.put("visitId", visit.getId().toString());
        detail.put("patientId", visit.getPatientId().toString());
        detail.put("viewedAt", viewedAt.toString());

        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize medical history detail audit.", exception);
        }
    }
}
