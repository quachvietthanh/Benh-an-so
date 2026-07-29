package com.benhsoan.persistence.mapper.medicalrecord;

import org.springframework.stereotype.Component;
import com.benhsoan.domain.medicalrecord.MedicalRecordDiagnosis;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordDiagnosisEntity;

@Component
public class MedicalRecordDiagnosisPersistenceMapper {

    public MedicalRecordDiagnosis toDomain(MedicalRecordDiagnosisEntity e) {
        return e == null ? null : MedicalRecordDiagnosis.restore(e.getId(), e.getMedicalRecordId(), e.getDiagnosisCatalogId(), e.getDiagnosisCode(), e.getDiagnosisName(), e.getDiagnosisType(), e.getNote(), e.getDiagnosedBy(), e.getDiagnosedAt(), e.getCreatedAt(), e.getUpdatedAt());
    }

    public MedicalRecordDiagnosisEntity toEntity(MedicalRecordDiagnosis d) {
        return d == null ? null : MedicalRecordDiagnosisEntity.builder().id(d.getId()).medicalRecordId(d.getMedicalRecordId()).diagnosisCatalogId(d.getDiagnosisCatalogId()).diagnosisCode(d.getDiagnosisCode()).diagnosisName(d.getDiagnosisName()).diagnosisType(d.getDiagnosisType()).note(d.getNote()).diagnosedBy(d.getDiagnosedBy()).diagnosedAt(d.getDiagnosedAt()).createdAt(d.getCreatedAt()).updatedAt(d.getUpdatedAt()).build();
    }
}
