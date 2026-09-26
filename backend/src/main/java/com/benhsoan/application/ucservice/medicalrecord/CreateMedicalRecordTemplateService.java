package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.MedicalRecordTemplate;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateNameDuplicateException;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.specialty.exception.SpecialtyNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.command.medicalrecord.CreateMedicalRecordTemplateCommand;
import com.benhsoan.port.dto.result.MedicalRecordTemplateResult;
import com.benhsoan.port.inbound.medicalrecord.CreateMedicalRecordTemplateUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordTemplateRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateMedicalRecordTemplateService implements CreateMedicalRecordTemplateUseCase {

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
    public MedicalRecordTemplateResult create(CreateMedicalRecordTemplateCommand command) {
        if (command == null || command.specialtyId() == null) {
            throw new ValidationException("Specialty id is required.");
        }
        Specialty specialty = specialtyRepository.findById(command.specialtyId())
                .filter(Specialty::isActive)
                .orElseThrow(() -> new SpecialtyNotFoundException(command.specialtyId()));
        Instant now = clockPort.now();
        UUID actorId = currentUserPort.getCurrentUserId();
        boolean makeDefault = command.makeDefault()
                || templateRepository.findBySpecialtyIdAndActive(specialty.getId(), true).isEmpty();
        MedicalRecordTemplate template = MedicalRecordTemplate.create(specialty.getId(), command.name(), makeDefault,
                commandMapper.toSectionDefinitions(command.sections()), actorId, now);
        if (templateRepository.existsBySpecialtyIdAndNameKey(template.getSpecialtyId(), template.getNameKey())) {
            throw new MedicalRecordTemplateNameDuplicateException(template.getName());
        }
        try {
            MedicalRecordTemplate saved = templateRepository.save(template);
            auditWriter.writeCreated(actorId, saved, now);
            return resultMapper.toResult(saved, specialty);
        } catch (DataIntegrityViolationException exception) {
            throw new MedicalRecordTemplateNameDuplicateException(template.getName());
        }
    }
}
