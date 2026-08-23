package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAlreadyLockedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordInvalidVisitException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordMissingDiagnosisException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordUnauthorizedSignerException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.medicalrecord.SignMedicalRecordCommand;
import com.benhsoan.port.dto.result.MedicalRecordResult;
import com.benhsoan.port.inbound.medicalrecord.SignMedicalRecordUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordDiagnosisRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SignMedicalRecordService implements SignMedicalRecordUseCase {

    private final MedicalRecordRepository medicalRecordRepository;
    private final VisitRepository visitRepository;
    private final MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;
    private final MedicalRecordAuthorizationService authorizationService;
    private final MedicalRecordAccessAuditService accessAuditService;
    private final MedicalRecordResultMapper resultMapper;
    private final ClockPort clockPort;

    @Override
    public MedicalRecordResult sign(UUID medicalRecordId, SignMedicalRecordCommand command) {
        UUID userId = authorizationService.requireWriteAccess();

        MedicalRecord record = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(medicalRecordId));

        if (record.isContentLocked()) {
            throw new MedicalRecordAlreadyLockedException();
        }

        Visit visit = visitRepository.findById(record.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(record.getVisitId()));

        if (!visit.isActive()) {
            throw new MedicalRecordInvalidVisitException(visit.getId());
        }

        Instant now = clockPort.now();

        if (!visit.getDoctorId().equals(userId)) {
            accessAuditService.recordRecordAccess(
                    visit.getPatientId(),
                    visit.getId(),
                    record.getId(),
                    userId,
                    MedicalRecordAccessAction.SIGN,
                    "Signature rejected: User is not doctor in charge",
                    now
            );
            throw new MedicalRecordUnauthorizedSignerException(medicalRecordId, visit.getDoctorId());
        }

        boolean hasDiagnosis = medicalRecordDiagnosisRepository.existsByMedicalRecordId(record.getId());
        if (!hasDiagnosis) {
            throw new MedicalRecordMissingDiagnosisException(record.getId());
        }

        String signatureData = (command != null && command.signatureData() != null && !command.signatureData().isBlank())
                ? command.signatureData().trim()
                : "SIMULATED_SIGNATURE:" + userId + ":" + now.toEpochMilli();

        record.sign(signatureData, userId, now);
        MedicalRecord saved = medicalRecordRepository.save(record);

        accessAuditService.recordRecordAccess(
                visit.getPatientId(),
                visit.getId(),
                saved.getId(),
                userId,
                MedicalRecordAccessAction.SIGN,
                "Medical record signed",
                now
        );

        return resultMapper.toResult(saved);
    }
}
