package com.benhsoan.application.ucservice.medicalrecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.auth.exception.UserNotFoundException;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.exception.PatientNotFoundException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.result.MedicalRecordDetailResult;
import com.benhsoan.port.dto.result.MedicalRecordResult;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordUseCase;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

/**
 * Query service for reviewing medical record content (NCL-04-CN-004).
 * Enforces read authorization (QTN-01) and writes an audit log for every
 * read access (QTN-02).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMedicalRecordService implements GetMedicalRecordUseCase {

    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;
    private final VisitRepository visitRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final MedicalRecordAuthorizationService authorizationService;
    private final MedicalRecordAccessAuditService accessAuditService;
    private final MedicalRecordTemplateApplicationMapper templateApplicationMapper;
    private final MedicalRecordResultMapper resultMapper;
    private final ClockPort clockPort;

    @Override
    public MedicalRecordResult getById(UUID medicalRecordId) {
        return getMedicalRecord(loadById(medicalRecordId));
    }

    @Override
    public MedicalRecordResult getByVisitId(UUID visitId) {
        MedicalRecord record = medicalRecordRepository.findByVisitId(visitId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(visitId));
        return getMedicalRecord(record);
    }

    @Override
    public MedicalRecordDetailResult getDetailByVisitId(UUID visitId) {
        MedicalRecord record = medicalRecordRepository.findByVisitId(visitId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(visitId));
        return getMedicalRecordDetail(record);
    }

    @Override
    public List<MedicalRecordDetailResult> getHistoryByPatientId(UUID patientId) {
        UUID userId = authorizationService.requireReadAccess();
        accessAuditService.recordHistoryView(patientId, userId, clockPort.now());
        return visitRepository.findByPatientIdOrderByVisitAtDesc(patientId).stream()
                .map(visit -> medicalRecordRepository.findByVisitId(visit.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::getMedicalRecordDetail)
                .toList();
    }

    private MedicalRecord loadById(UUID medicalRecordId) {
        return medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(medicalRecordId));
    }

    private MedicalRecordResult getMedicalRecord(MedicalRecord record) {
        UUID userId = authorizationService.requireReadAccess();
        var visit = visitRepository.findById(record.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(record.getVisitId()));
        accessAuditService.recordRecordView(visit.getPatientId(), visit.getId(), record.getId(), userId, clockPort.now());
        var appliedTemplate = templateApplicationMapper.resolveApplied(record, visit);
        return appliedTemplate == null ? resultMapper.toResult(record) : resultMapper.toResult(record, appliedTemplate);
    }

    private MedicalRecordDetailResult getMedicalRecordDetail(MedicalRecord record) {
        UUID userId = authorizationService.requireReadAccess();
        Visit visit = visitRepository.findById(record.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(record.getVisitId()));
        Patient patient = patientRepository.findById(visit.getPatientId())
                .orElseThrow(() -> new PatientNotFoundException(visit.getPatientId()));
        User doctor = userRepository.findById(visit.getDoctorId())
                .orElseThrow(() -> new UserNotFoundException(visit.getDoctorId().toString()));
        accessAuditService.recordRecordView(visit.getPatientId(), visit.getId(), record.getId(), userId, clockPort.now());
        var diagnoses = medicalRecordDiagnosisRepository.findByMedicalRecordId(record.getId());
        var appliedTemplate = templateApplicationMapper.resolveApplied(record, visit);
        return appliedTemplate == null
                ? resultMapper.toDetailResult(record, visit, patient, doctor, diagnoses)
                : resultMapper.toDetailResult(record, visit, patient, doctor, diagnoses, appliedTemplate);
    }
}
