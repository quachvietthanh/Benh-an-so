package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.MedicalRecordTemplate;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateDefaultReplacementRequiredException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateInvalidReplacementException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateLastActiveException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateNotFoundException;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.specialty.exception.SpecialtyNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.command.medicalrecord.UpdateMedicalRecordTemplateStatusCommand;
import com.benhsoan.port.dto.result.MedicalRecordTemplateResult;
import com.benhsoan.port.inbound.medicalrecord.UpdateMedicalRecordTemplateStatusUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordTemplateRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateMedicalRecordTemplateStatusService implements UpdateMedicalRecordTemplateStatusUseCase {

    private static final String MANAGE_PERMISSION = "MEDICAL_RECORD_TEMPLATE_MANAGE";

    private final MedicalRecordTemplateRepository templateRepository;
    private final SpecialtyRepository specialtyRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final MedicalRecordTemplateResultMapper resultMapper;
    private final MedicalRecordTemplateAuditWriter auditWriter;

    @Override
    @RequirePermission(MANAGE_PERMISSION)
    public MedicalRecordTemplateResult updateStatus(UpdateMedicalRecordTemplateStatusCommand command) {
        if (command == null || command.templateId() == null) {
            throw new ValidationException("Medical record template id is required.");
        }
        MedicalRecordTemplate template = templateRepository.findById(command.templateId())
                .orElseThrow(() -> new MedicalRecordTemplateNotFoundException(command.templateId()));
        if (template.isActive() == command.active()) {
            return resultMapper.toResult(template, specialtyFor(template));
        }

        Instant now = clockPort.now();
        UUID actorId = currentUserPort.getCurrentUserId();
        MedicalRecordTemplate saved;
        if (command.active()) {
            template.activate(actorId, now);
            saved = templateRepository.save(template);
        } else {
            validateDeactivation(template, command.replacementTemplateId());
            saved = templateRepository.deactivate(template.getId(), command.replacementTemplateId(), actorId, now);
        }
        auditWriter.writeStatusChanged(actorId, saved, now);
        return resultMapper.toResult(saved, specialtyFor(saved));
    }

    private void validateDeactivation(MedicalRecordTemplate template, UUID replacementTemplateId) {
        if (templateRepository.findBySpecialtyIdAndActive(template.getSpecialtyId(), true).size() <= 1) {
            throw new MedicalRecordTemplateLastActiveException();
        }
        if (!template.isDefaultTemplate()) {
            return;
        }
        if (replacementTemplateId == null) {
            throw new MedicalRecordTemplateDefaultReplacementRequiredException();
        }
        if (replacementTemplateId.equals(template.getId())) {
            throw new MedicalRecordTemplateInvalidReplacementException();
        }
        MedicalRecordTemplate replacement = templateRepository.findById(replacementTemplateId)
                .orElseThrow(MedicalRecordTemplateInvalidReplacementException::new);
        if (!replacement.isActive() || !replacement.getSpecialtyId().equals(template.getSpecialtyId())) {
            throw new MedicalRecordTemplateInvalidReplacementException();
        }
    }

    private Specialty specialtyFor(MedicalRecordTemplate template) {
        return specialtyRepository.findById(template.getSpecialtyId())
                .orElseThrow(() -> new SpecialtyNotFoundException(template.getSpecialtyId()));
    }
}
