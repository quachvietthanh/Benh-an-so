package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordRetentionException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.inbound.medicalrecord.DeleteMedicalRecordUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DeleteMedicalRecordService implements DeleteMedicalRecordUseCase {

    private final MedicalRecordRepository medicalRecordRepository;
    private final VisitRepository visitRepository;
    private final MedicalRecordDeletionAuditWriter auditWriter;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final MedicalRecordRetentionPolicy retentionPolicy;

    @Override
    public void delete(UUID medicalRecordId) {
        MedicalRecord record = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(medicalRecordId));
        Visit visit = visitRepository.findById(record.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(record.getVisitId()));

        Instant now = clockPort.now();
        UUID actorId = currentUserPort.getCurrentUserId();

        if (retentionPolicy.isWithinRetention(visit, now)) {
            auditWriter.writeDenied(actorId, medicalRecordId, now);
            throw new MedicalRecordRetentionException();
        }

        medicalRecordRepository.deleteById(medicalRecordId);
        auditWriter.writeDeleted(actorId, medicalRecordId, now);
    }
}
