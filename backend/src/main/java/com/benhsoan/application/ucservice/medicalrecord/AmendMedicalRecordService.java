package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordAmendment;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAccessDeniedException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordAmendmentRequiresCompletedVisitException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotLockedException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.medicalrecord.AmendMedicalRecordCommand;
import com.benhsoan.port.dto.result.MedicalRecordAmendmentResult;
import com.benhsoan.port.inbound.medicalrecord.AmendMedicalRecordUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordAmendmentRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
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
    private final CurrentUserPort currentUserPort;
    private final MedicalRecordAmendmentAuditWriter amendmentAuditWriter;

    @Override
    public MedicalRecordAmendmentResult amend(UUID medicalRecordId, AmendMedicalRecordCommand command) {
        MedicalRecord record = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(medicalRecordId));

        validateCommand(command);

        if (!record.isContentLocked()) {
            throw new MedicalRecordNotLockedException();
        }

        Visit visit = visitRepository.findById(record.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(record.getVisitId()));
        if (!visit.isCompleted()) {
            throw new MedicalRecordAmendmentRequiresCompletedVisitException();
        }

        UUID userId = authorizeAmend(record, visit);
        Instant now = clockPort.now();

        MedicalRecordAmendment saved = amendmentRepository.save(MedicalRecordAmendment.create(
                record.getId(), command.content(), command.reason(), userId, now
        ));

        amendmentAuditWriter.writeAmended(userId, record.getId(), command.reason(), now);
        accessAuditService.recordRecordAccess(visit.getPatientId(), visit.getId(), record.getId(), userId,
                MedicalRecordAccessAction.AMEND, "Medical record amended", now);
        return resultMapper.toResult(saved);
    }

    private void validateCommand(AmendMedicalRecordCommand command) {
        if (command == null || isBlank(command.reason())) {
            throw new ValidationException("Yêu cầu nhập lý do đính chính.");
        }
        if (isBlank(command.content())) {
            throw new ValidationException("Amendment content is required.");
        }
    }

    private UUID authorizeAmend(MedicalRecord record, Visit visit) {
        UUID actorId = currentUserPort.getCurrentUserId();
        try {
            authorizationService.requireAmendAccess(visit.getDoctorId());
            return actorId;
        } catch (MedicalRecordAccessDeniedException denied) {
            amendmentAuditWriter.writeDenied(actorId, record.getId(),
                    "Medical record amendment denied: not the responsible doctor.", clockPort.now());
            throw denied;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
