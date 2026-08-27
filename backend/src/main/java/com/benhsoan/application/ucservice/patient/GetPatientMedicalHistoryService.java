package com.benhsoan.application.ucservice.patient;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.dto.result.patient.PatientMedicalHistorySummaryResult;
import com.benhsoan.port.inbound.patient.GetPatientMedicalHistoryUseCase;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-005 CV-01/CV-02: lists the authenticated patient's completed visits whose
 * medical record is finalized (SIGNED/LOCKED/ARCHIVED), ordered by visit date DESC.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPatientMedicalHistoryService implements GetPatientMedicalHistoryUseCase {

    private final PatientRepository patientRepository;
    private final VisitRepository visitRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordDiagnosisRepository diagnosisRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final UserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;
    private final CurrentUserPort currentUserPort;

    @Override
    public List<PatientMedicalHistorySummaryResult> getMedicalHistory() {
        Patient patient = patientRepository.findByUserId(currentUserPort.getCurrentUserId())
                .orElseThrow(() -> new AccessDeniedException(
                        "No patient profile is linked to the authenticated user."));

        return visitRepository.findByPatientIdOrderByVisitAtDesc(patient.getId()).stream()
                .filter(Visit::isCompleted)
                .map(visit -> toSummary(visit))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private PatientMedicalHistorySummaryResult toSummary(Visit visit) {
        MedicalRecord record = medicalRecordRepository.findByVisitId(visit.getId())
                .filter(MedicalRecord::isContentLocked)
                .orElse(null);
        if (record == null) {
            return null;
        }

        List<MedicalRecordDiagnosis> diagnoses = diagnosisRepository.findByMedicalRecordId(record.getId());
        String diagnosisSummary = diagnoses.isEmpty()
                ? record.getConclusion()
                : diagnoses.stream()
                        .map(MedicalRecordDiagnosis::getDiagnosisName)
                        .collect(Collectors.joining(", "));

        int prescriptionCount = prescriptionRepository.findByMedicalRecordId(record.getId()).size();

        return new PatientMedicalHistorySummaryResult(
                visit.getId(),
                visit.getVisitAt(),
                userRepository.findById(visit.getDoctorId()).map(User::getFullName).orElse(null),
                specialtyRepository.findById(visit.getSpecialtyId()).map(Specialty::getName).orElse(null),
                diagnosisSummary,
                prescriptionCount
        );
    }
}
