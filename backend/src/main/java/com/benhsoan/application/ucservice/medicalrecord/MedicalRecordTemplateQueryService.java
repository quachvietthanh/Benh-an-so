package com.benhsoan.application.ucservice.medicalrecord;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicalrecord.MedicalRecordTemplate;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateNotFoundException;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.specialty.exception.SpecialtyNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.command.medicalrecord.SearchMedicalRecordTemplateQuery;
import com.benhsoan.port.dto.result.MedicalRecordTemplateResult;
import com.benhsoan.port.inbound.medicalrecord.MedicalRecordTemplateQueryUseCase;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordTemplateRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicalRecordTemplateQueryService implements MedicalRecordTemplateQueryUseCase {

    private static final String MANAGE_PERMISSION = "MEDICAL_RECORD_TEMPLATE_MANAGE";

    private final MedicalRecordTemplateRepository templateRepository;
    private final SpecialtyRepository specialtyRepository;
    private final MedicalRecordTemplateResultMapper resultMapper;

    @Override
    @RequirePermission(MANAGE_PERMISSION)
    public List<MedicalRecordTemplateResult> search(SearchMedicalRecordTemplateQuery query) {
        SearchMedicalRecordTemplateQuery effectiveQuery = query == null
                ? new SearchMedicalRecordTemplateQuery(null, null) : query;
        return templateRepository.search(effectiveQuery.specialtyId(), effectiveQuery.active()).stream()
                .map(template -> resultMapper.toResult(template, specialtyFor(template)))
                .toList();
    }

    @Override
    @RequirePermission(MANAGE_PERMISSION)
    public MedicalRecordTemplateResult getById(UUID templateId) {
        if (templateId == null) {
            throw new ValidationException("Medical record template id is required.");
        }
        MedicalRecordTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new MedicalRecordTemplateNotFoundException(templateId));
        return resultMapper.toResult(template, specialtyFor(template));
    }

    private Specialty specialtyFor(MedicalRecordTemplate template) {
        return specialtyRepository.findById(template.getSpecialtyId())
                .orElseThrow(() -> new SpecialtyNotFoundException(template.getSpecialtyId()));
    }
}
