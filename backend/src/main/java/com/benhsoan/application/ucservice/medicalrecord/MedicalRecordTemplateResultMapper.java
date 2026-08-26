package com.benhsoan.application.ucservice.medicalrecord;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.medicalrecord.MedicalRecordTemplate;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.port.dto.result.MedicalRecordTemplateResult;
import com.benhsoan.port.dto.result.MedicalRecordTemplateSectionResult;
import com.benhsoan.port.dto.result.SpecialtyResult;

@Component
public class MedicalRecordTemplateResultMapper {

    public MedicalRecordTemplateResult toResult(MedicalRecordTemplate template, Specialty specialty) {
        return new MedicalRecordTemplateResult(template.getId(), toResult(specialty), template.getName(),
                template.isActive(), template.isDefaultTemplate(), template.getCurrentVersionNo(),
                template.getCurrentVersion().getSections().stream()
                        .map(section -> new MedicalRecordTemplateSectionResult(section.getFieldCode(), section.getLabel(),
                                section.isRequired(), section.getDisplayOrder()))
                        .toList(),
                template.getCreatedAt(), template.getUpdatedAt());
    }

    public SpecialtyResult toResult(Specialty specialty) {
        return new SpecialtyResult(specialty.getId(), specialty.getCode(), specialty.getName(), specialty.isActive());
    }
}
