package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.medicalrecord.UpdateMedicalRecordCommand;
import com.benhsoan.port.dto.result.MedicalRecordResult;
import com.benhsoan.port.inbound.medicalrecord.UpdateMedicalRecordUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.crudRepository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateMedicalRecordService implements UpdateMedicalRecordUseCase {

    private final MedicalRecordRepository medicalRecordRepository;
    private final VisitRepository visitRepository;
    private final MedicalRecordAuthorizationService authorizationService;
    private final MedicalRecordAccessAuditService accessAuditService;
    private final MedicalRecordResultMapper resultMapper;
    private final ClockPort clockPort;

    @Override
    public MedicalRecordResult update(UUID medicalRecordId, UpdateMedicalRecordCommand command) {
        UUID userId = authorizationService.requireWriteAccess();
        MedicalRecord record = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(medicalRecordId));
        var visit = visitRepository.findById(record.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(record.getVisitId()));
        Instant now = clockPort.now();
        record.updateContent(command.chiefComplaint(), command.symptoms(), command.medicalHistory(),
                command.physicalExamination(), command.clinicalProgress(), command.treatmentPlan(),
                command.doctorInstructions(), command.conclusion(), userId, now);
        MedicalRecord saved = medicalRecordRepository.save(record);
        accessAuditService.recordRecordAccess(visit.getPatientId(), visit.getId(), saved.getId(), userId,
                MedicalRecordAccessAction.UPDATE, "Medical record updated", now);
        return resultMapper.toResult(saved);
    }
}
