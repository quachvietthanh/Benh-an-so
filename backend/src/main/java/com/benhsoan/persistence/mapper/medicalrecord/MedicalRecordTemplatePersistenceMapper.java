package com.benhsoan.persistence.mapper.medicalrecord;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.medicalrecord.MedicalRecordTemplate;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordTemplateEntity;

@Component
public class MedicalRecordTemplatePersistenceMapper {

    public MedicalRecordTemplateEntity toEntity(MedicalRecordTemplate template) {
        return template == null ? null : MedicalRecordTemplateEntity.builder()
                .id(template.getId()).specialtyId(template.getSpecialtyId()).name(template.getName())
                .nameKey(template.getNameKey()).active(template.isActive()).defaultTemplate(template.isDefaultTemplate())
                .currentVersionNo(template.getCurrentVersionNo()).createdBy(template.getCreatedBy())
                .createdAt(template.getCreatedAt()).updatedBy(template.getUpdatedBy()).updatedAt(template.getUpdatedAt())
                .build();
    }
}
