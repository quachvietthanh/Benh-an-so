package com.benhsoan.application.ucservice.medicalrecord;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.result.MedicalRecordDiagnosisResult;
import com.benhsoan.port.inbound.medicalrecord.GetMedicalRecordDiagnosesUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMedicalRecordDiagnosesService implements GetMedicalRecordDiagnosesUseCase {

    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;
    private final VisitRepository visitRepository;
    private final MedicalRecordAuthorizationService authorizationService;
    private final MedicalRecordAccessAuditService accessAuditService;
    private final MedicalRecordDiagnosisResultMapper resultMapper;
    private final ClockPort clockPort;

    @Override
    public List<MedicalRecordDiagnosisResult> getByMedicalRecordId(UUID medicalRecordId) {
        UUID actorId = authorizationService.requireReadAccess();
        MedicalRecord record = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(medicalRecordId));
        var visit = visitRepository.findById(record.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(record.getVisitId()));
        accessAuditService.recordRecordView(visit.getPatientId(), visit.getId(), record.getId(), actorId, clockPort.now());
        return medicalRecordDiagnosisRepository.findByMedicalRecordId(record.getId()).stream()
                .map(resultMapper::toResult)
                .toList();
    }
}
