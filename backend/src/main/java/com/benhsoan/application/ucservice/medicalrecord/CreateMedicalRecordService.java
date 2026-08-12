package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAlreadyExistsForVisitException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordInvalidVisitException;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.medicalrecord.CreateMedicalRecordCommand;
import com.benhsoan.port.dto.result.MedicalRecordResult;
import com.benhsoan.port.inbound.medicalrecord.CreateMedicalRecordUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateMedicalRecordService implements CreateMedicalRecordUseCase {

    private final MedicalRecordRepository medicalRecordRepository;
    private final VisitRepository visitRepository;
    private final MedicalRecordAuthorizationService authorizationService;
    private final MedicalRecordAccessAuditService accessAuditService;
    private final MedicalRecordResultMapper resultMapper;
    private final ClockPort clockPort;

    @Override
    public MedicalRecordResult create(CreateMedicalRecordCommand command) {
        var userId = authorizationService.requireWriteAccess();
        var visit = visitRepository.findById(command.visitId())
                .orElseThrow(() -> new VisitNotFoundException(command.visitId()));
        if (!visit.isActive()) {
            throw new MedicalRecordInvalidVisitException(visit.getId());
        }
        if (medicalRecordRepository.existsByVisitId(visit.getId())) {
            throw new MedicalRecordAlreadyExistsForVisitException(visit.getId());
        }

        Instant now = clockPort.now();
        MedicalRecord saved = medicalRecordRepository.save(MedicalRecord.create(
                visit.getId(), command.chiefComplaint(), command.symptoms(), command.medicalHistory(),
                command.physicalExamination(), command.clinicalProgress(), command.treatmentPlan(),
                command.doctorInstructions(), command.conclusion(), userId, now
        ));
        accessAuditService.recordRecordAccessInCurrentTransaction(visit.getPatientId(), visit.getId(), saved.getId(), userId,
                MedicalRecordAccessAction.CREATE, "Medical record created", now);
        return resultMapper.toResult(saved);
    }
}
