package com.benhsoan.persistence.mapper.medicalrecord;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.medicalrecord.MedicalRecordTemplateSection;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordTemplateSectionEntity;

@Component
public class MedicalRecordTemplateSectionPersistenceMapper {

    public MedicalRecordTemplateSection toDomain(MedicalRecordTemplateSectionEntity entity) {
        return MedicalRecordTemplateSection.restore(entity.getId(), entity.getTemplateVersionId(), entity.getFieldCode(),
                entity.getLabel(), entity.isRequired(), entity.getDisplayOrder());
    }

    public MedicalRecordTemplateSectionEntity toEntity(MedicalRecordTemplateSection section) {
        return MedicalRecordTemplateSectionEntity.builder().id(section.getId())
                .templateVersionId(section.getTemplateVersionId()).fieldCode(section.getFieldCode())
                .label(section.getLabel()).required(section.isRequired()).displayOrder(section.getDisplayOrder()).build();
    }
}
