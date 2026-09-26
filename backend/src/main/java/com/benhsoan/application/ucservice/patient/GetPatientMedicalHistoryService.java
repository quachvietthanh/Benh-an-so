package com.benhsoan.application.ucservice.patient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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

        List<Visit> visits = visitRepository.findByPatientIdOrderByVisitAtDesc(patient.getId());
        if (visits.isEmpty()) {
            return List.of();
        }

        List<UUID> visitIds = visits.stream().map(Visit::getId).toList();

        Map<UUID, MedicalRecord> recordsByVisitId = medicalRecordRepository.findByVisitIdIn(visitIds).stream()
                .filter(MedicalRecord::isContentLocked)
                .collect(Collectors.toMap(MedicalRecord::getVisitId, record -> record, (a, b) -> a));

        List<UUID> recordIds = recordsByVisitId.values().stream().map(MedicalRecord::getId).toList();

        Map<UUID, List<MedicalRecordDiagnosis>> diagnosesByRecordId = diagnosisRepository
                .findByMedicalRecordIdIn(recordIds).stream()
                .collect(Collectors.groupingBy(MedicalRecordDiagnosis::getMedicalRecordId));

        Map<UUID, Long> prescriptionCounts = prescriptionRepository.countByMedicalRecordIdIn(recordIds);

        Map<UUID, String> doctorNames = userRepository.findAllById(
                toList(collectNonNull(visits.stream().map(Visit::getDoctorId).collect(Collectors.toSet())))
        ).stream().collect(Collectors.toMap(User::getId, User::getFullName, (a, b) -> a));

        Map<UUID, String> specialtyNames = specialtyRepository.findAllById(
                toList(collectNonNull(visits.stream().map(Visit::getSpecialtyId).collect(Collectors.toSet())))
        ).stream().collect(Collectors.toMap(Specialty::getId, Specialty::getName, (a, b) -> a));

        return visits.stream()
                .filter(visit -> recordsByVisitId.containsKey(visit.getId()))
                .map(visit -> toSummary(
                        visit,
                        recordsByVisitId.get(visit.getId()),
                        diagnosesByRecordId,
                        prescriptionCounts,
                        doctorNames,
                        specialtyNames))
                .toList();
    }

    private PatientMedicalHistorySummaryResult toSummary(
            Visit visit,
            MedicalRecord record,
            Map<UUID, List<MedicalRecordDiagnosis>> diagnosesByRecordId,
            Map<UUID, Long> prescriptionCounts,
            Map<UUID, String> doctorNames,
            Map<UUID, String> specialtyNames
    ) {
        List<MedicalRecordDiagnosis> diagnoses = diagnosesByRecordId.getOrDefault(record.getId(), List.of());
        String diagnosisSummary = diagnoses.isEmpty()
                ? record.getConclusion()
                : diagnoses.stream()
                        .map(MedicalRecordDiagnosis::getDiagnosisName)
                        .collect(Collectors.joining(", "));

        int prescriptionCount = prescriptionCounts.getOrDefault(record.getId(), 0L).intValue();

        return new PatientMedicalHistorySummaryResult(
                visit.getId(),
                visit.getVisitAt(),
                doctorNames.get(visit.getDoctorId()),
                specialtyNames.get(visit.getSpecialtyId()),
                diagnosisSummary,
                prescriptionCount
        );
    }

    private static Set<UUID> collectNonNull(Set<UUID> ids) {
        return ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static List<UUID> toList(Collection<UUID> ids) {
        return ids == null ? List.of() : new ArrayList<>(ids);
    }
}
