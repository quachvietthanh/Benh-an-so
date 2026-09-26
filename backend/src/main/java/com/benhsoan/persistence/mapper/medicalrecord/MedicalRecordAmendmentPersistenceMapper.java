package com.benhsoan.persistence.mapper.medicalrecord;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.medicalrecord.MedicalRecordAmendment;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAmendmentEntity;

@Component
public class MedicalRecordAmendmentPersistenceMapper {

    public MedicalRecordAmendment toDomain(MedicalRecordAmendmentEntity e) {
        return e == null ? null : MedicalRecordAmendment.restore(e.getId(), e.getMedicalRecordId(), e.getContent(), e.getReason(), e.getAmendedBy(), e.getAmendedAt());
    }

    public MedicalRecordAmendmentEntity toEntity(MedicalRecordAmendment d) {
        return d == null ? null : MedicalRecordAmendmentEntity.builder().id(d.getId()).medicalRecordId(d.getMedicalRecordId()).content(d.getContent()).reason(d.getReason()).amendedBy(d.getAmendedBy()).amendedAt(d.getAmendedAt()).build();
    }
}