package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordAmendment;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotLockedException;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.medicalrecord.AmendMedicalRecordCommand;
import com.benhsoan.port.dto.result.MedicalRecordAmendmentResult;
import com.benhsoan.port.inbound.medicalrecord.AmendMedicalRecordUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.medicalrecord.MedicalRecordAmendmentRepository;
import com.benhsoan.port.outbound.repository.crudRepository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.crudRepository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AmendMedicalRecordService implements AmendMedicalRecordUseCase {

    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordAmendmentRepository amendmentRepository;
    private final VisitRepository visitRepository;
    private final MedicalRecordAuthorizationService authorizationService;
    private final MedicalRecordAccessAuditService accessAuditService;
    private final MedicalRecordResultMapper resultMapper;
    private final ClockPort clockPort;

    @Override
    public MedicalRecordAmendmentResult amend(UUID medicalRecordId, AmendMedicalRecordCommand command) {
        UUID userId = authorizationService.requireWriteAccess();

        MedicalRecord record = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(medicalRecordId));

        if (!record.isLocked()) {
            throw new MedicalRecordNotLockedException();
        }

        var visit = visitRepository.findById(record.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(record.getVisitId()));

        Instant now = clockPort.now();

        MedicalRecordAmendment saved = amendmentRepository.save(MedicalRecordAmendment.create(
                record.getId(), command.content(), command.reason(), userId, now
        ));
        
        accessAuditService.recordRecordAccess(visit.getPatientId(), visit.getId(), record.getId(), userId,
                MedicalRecordAccessAction.AMEND, "Medical record amended", now);
        return resultMapper.toResult(saved);
    }
}
