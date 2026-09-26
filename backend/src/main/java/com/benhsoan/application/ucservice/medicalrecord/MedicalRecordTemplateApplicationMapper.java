package com.benhsoan.application.ucservice.medicalrecord;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.MedicalRecordTemplate;
import com.benhsoan.domain.medicalrecord.MedicalRecordTemplateVersion;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordTemplateNotFoundException;
import com.benhsoan.domain.specialty.Specialty;
import com.benhsoan.domain.specialty.exception.SpecialtyNotFoundException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.dto.result.AppliedMedicalRecordTemplateResult;
import com.benhsoan.port.dto.result.MedicalRecordTemplateOptionResult;
import com.benhsoan.port.dto.result.MedicalRecordTemplateSectionResult;
import com.benhsoan.port.dto.result.SpecialtyResult;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordTemplateRepository;
import com.benhsoan.port.outbound.repository.specialty.SpecialtyRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MedicalRecordTemplateApplicationMapper {

    private final MedicalRecordTemplateRepository templateRepository;
    private final SpecialtyRepository specialtyRepository;

    public MedicalRecordTemplateOptionResult toOption(MedicalRecordTemplate template) {
        return toOption(template, template.getCurrentVersion());
    }

    public MedicalRecordTemplateOptionResult toOption(MedicalRecordTemplate template, MedicalRecordTemplateVersion version) {
        return new MedicalRecordTemplateOptionResult(template.getId(), version.getId(), toSpecialty(version.getSpecialtyId()),
                version.getTemplateName(), version.getVersionNo(), template.isDefaultTemplate(), toSections(version));
    }

    public AppliedMedicalRecordTemplateResult resolveApplied(MedicalRecord record, Visit visit) {
        if (record.getAppliedTemplateVersionId() == null) return null;
        MedicalRecordTemplateVersion version = templateRepository.findVersionById(record.getAppliedTemplateVersionId())
                .orElseThrow(() -> new MedicalRecordTemplateNotFoundException(record.getAppliedTemplateVersionId()));
        boolean fallback = !version.getSpecialtyId().equals(visit.getSpecialtyId());
        return new AppliedMedicalRecordTemplateResult(version.getTemplateId(), version.getId(),
                toSpecialty(version.getSpecialtyId()), version.getTemplateName(), version.getVersionNo(),
                toSections(version), record.getTemplateAppliedBy(), record.getTemplateAppliedAt(), fallback);
    }

    public SpecialtyResult toSpecialty(Specialty specialty) {
        return new SpecialtyResult(specialty.getId(), specialty.getCode(), specialty.getName(), specialty.isActive());
    }

    private SpecialtyResult toSpecialty(java.util.UUID specialtyId) {
        Specialty specialty = specialtyRepository.findById(specialtyId)
                .orElseThrow(() -> new SpecialtyNotFoundException(specialtyId));
        return toSpecialty(specialty);
    }

    private java.util.List<MedicalRecordTemplateSectionResult> toSections(MedicalRecordTemplateVersion version) {
        return version.getSections().stream().map(section -> new MedicalRecordTemplateSectionResult(
                section.getFieldCode(), section.getLabel(), section.isRequired(), section.getDisplayOrder())).toList();
    }
}
