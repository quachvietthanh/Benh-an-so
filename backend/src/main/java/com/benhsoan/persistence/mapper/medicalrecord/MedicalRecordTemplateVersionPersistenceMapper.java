package com.benhsoan.persistence.mapper.medicalrecord;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.medicalrecord.MedicalRecordTemplateSection;
import com.benhsoan.domain.medicalrecord.MedicalRecordTemplateVersion;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordTemplateVersionEntity;

@Component
public class MedicalRecordTemplateVersionPersistenceMapper {

    public MedicalRecordTemplateVersion toDomain(MedicalRecordTemplateVersionEntity entity,
            List<MedicalRecordTemplateSection> sections) {
        return MedicalRecordTemplateVersion.restore(entity.getId(), entity.getTemplateId(), entity.getVersionNo(),
                entity.getSpecialtyId(), entity.getTemplateName(), entity.getChangeNote(), entity.getCreatedBy(),
                entity.getCreatedAt(), sections);
    }

    public MedicalRecordTemplateVersionEntity toEntity(MedicalRecordTemplateVersion version) {
        return MedicalRecordTemplateVersionEntity.builder().id(version.getId()).templateId(version.getTemplateId())
                .versionNo(version.getVersionNo()).specialtyId(version.getSpecialtyId())
                .templateName(version.getTemplateName()).changeNote(version.getChangeNote())
                .createdBy(version.getCreatedBy()).createdAt(version.getCreatedAt()).build();
    }
}
