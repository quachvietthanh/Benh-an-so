package com.benhsoan.application.ucservice.medicalrecord;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.MedicalRecordTemplate;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateInactiveException;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateNotFoundException;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.specialty.exception.SpecialtyNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.result.MedicalRecordTemplateResult;
import com.benhsoan.port.inbound.medicalrecord.SetMedicalRecordTemplateDefaultUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordTemplateRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SetMedicalRecordTemplateDefaultService implements SetMedicalRecordTemplateDefaultUseCase {

    private static final String MANAGE_PERMISSION = "MEDICAL_RECORD_TEMPLATE_MANAGE";

    private final MedicalRecordTemplateRepository templateRepository;
    private final SpecialtyRepository specialtyRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final MedicalRecordTemplateResultMapper resultMapper;
    private final MedicalRecordTemplateAuditWriter auditWriter;

    @Override
    @RequirePermission(MANAGE_PERMISSION)
    public MedicalRecordTemplateResult setDefault(UUID templateId) {
        if (templateId == null) {
            throw new ValidationException("Medical record template id is required.");
        }
        MedicalRecordTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new MedicalRecordTemplateNotFoundException(templateId));
        if (!template.isActive()) {
            throw new MedicalRecordTemplateInactiveException();
        }
        Instant now = clockPort.now();
        UUID actorId = currentUserPort.getCurrentUserId();
        MedicalRecordTemplate saved = templateRepository.setDefault(template.getId(), actorId, now);
        auditWriter.writeDefaultSet(actorId, saved, now);
        return resultMapper.toResult(saved, specialtyFor(saved));
    }

    private Specialty specialtyFor(MedicalRecordTemplate template) {
        return specialtyRepository.findById(template.getSpecialtyId())
                .orElseThrow(() -> new SpecialtyNotFoundException(template.getSpecialtyId()));
    }
}
