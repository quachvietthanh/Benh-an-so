package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.MedicalRecordTemplate;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateNameDuplicateException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateNotFoundException;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.specialty.exception.SpecialtyNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.command.medicalrecord.UpdateMedicalRecordTemplateCommand;
import com.benhsoan.port.dto.result.MedicalRecordTemplateResult;
import com.benhsoan.port.inbound.medicalrecord.UpdateMedicalRecordTemplateUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordTemplateRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateMedicalRecordTemplateService implements UpdateMedicalRecordTemplateUseCase {

    private static final String MANAGE_PERMISSION = "MEDICAL_RECORD_TEMPLATE_MANAGE";

    private final MedicalRecordTemplateRepository templateRepository;
    private final SpecialtyRepository specialtyRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final MedicalRecordTemplateCommandMapper commandMapper;
    private final MedicalRecordTemplateResultMapper resultMapper;
    private final MedicalRecordTemplateAuditWriter auditWriter;

    @Override
    @RequirePermission(MANAGE_PERMISSION)
    public MedicalRecordTemplateResult update(UpdateMedicalRecordTemplateCommand command) {
        if (command == null || command.templateId() == null) {
            throw new ValidationException("Medical record template id is required.");
        }
        MedicalRecordTemplate template = templateRepository.findById(command.templateId())
                .orElseThrow(() -> new MedicalRecordTemplateNotFoundException(command.templateId()));
        String nameKey = normalizeNameKey(command.name());
        if (templateRepository.existsBySpecialtyIdAndNameKeyAndIdNot(template.getSpecialtyId(), nameKey, template.getId())) {
            throw new MedicalRecordTemplateNameDuplicateException(command.name());
        }
        Instant now = clockPort.now();
        UUID actorId = currentUserPort.getCurrentUserId();
        template.update(command.name(), commandMapper.toSectionDefinitions(command.sections()), command.changeNote(), actorId,
                now);
        try {
            MedicalRecordTemplate saved = templateRepository.save(template);
            auditWriter.writeUpdated(actorId, saved, now);
            return resultMapper.toResult(saved, specialtyFor(saved));
        } catch (DataIntegrityViolationException exception) {
            throw new MedicalRecordTemplateNameDuplicateException(command.name());
        }
    }

    private Specialty specialtyFor(MedicalRecordTemplate template) {
        return specialtyRepository.findById(template.getSpecialtyId())
                .orElseThrow(() -> new SpecialtyNotFoundException(template.getSpecialtyId()));
    }

    private static String normalizeNameKey(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Template name is required.");
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
