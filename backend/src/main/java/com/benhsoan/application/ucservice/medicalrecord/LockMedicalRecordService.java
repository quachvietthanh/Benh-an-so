package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordInvalidVisitException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.result.MedicalRecordResult;
import com.benhsoan.port.inbound.medicalrecord.LockMedicalRecordUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class LockMedicalRecordService implements LockMedicalRecordUseCase {

    private final MedicalRecordRepository medicalRecordRepository;
    private final VisitRepository visitRepository;
    private final MedicalRecordAuthorizationService authorizationService;
    private final MedicalRecordAccessAuditService accessAuditService;
    private final MedicalRecordTemplateApplicationMapper templateMapper;
    private final MedicalRecordResultMapper resultMapper;
    private final ClockPort clockPort;

    @Override
    public MedicalRecordResult lock(UUID medicalRecordId) {
        UUID userId = authorizationService.requireWriteAccess();
        MedicalRecord record = medicalRecordRepository.findByIdForUpdate(medicalRecordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(medicalRecordId));
        Visit visit = visitRepository.findById(record.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(record.getVisitId()));
        if (!visit.isActive()) {
            throw new MedicalRecordInvalidVisitException(visit.getId());
        }
        Instant now = clockPort.now();
        record.lock(userId, now);
        MedicalRecord saved = medicalRecordRepository.save(record);
        accessAuditService.recordRecordAccess(visit.getPatientId(), visit.getId(), saved.getId(), userId,
                MedicalRecordAccessAction.LOCK, "Medical record locked", now);
        return resultMapper.toResult(saved, templateMapper.resolveApplied(saved, visit));
    }
}
