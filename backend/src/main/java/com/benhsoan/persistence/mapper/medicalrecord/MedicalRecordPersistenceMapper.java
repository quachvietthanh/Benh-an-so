package com.benhsoan.persistence.mapper.medicalrecord;

import org.springframework.stereotype.Component;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordEntity;

@Component
public class MedicalRecordPersistenceMapper {

    public MedicalRecord toDomain(MedicalRecordEntity e) {
        return e == null ? null : MedicalRecord.restore(e.getId(), e.getVisitId(), e.getChiefComplaint(), e.getSymptoms(), e.getMedicalHistory(), e.getPhysicalExamination(), e.getClinicalProgress(), e.getTreatmentPlan(), e.getDoctorInstructions(), e.getConclusion(), e.getStatus(), e.getLockedAt(), e.getLockedBy(), e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedBy(), e.getUpdatedAt());
    }

    public MedicalRecordEntity toEntity(MedicalRecord d) {
        return d == null ? null : MedicalRecordEntity.builder().id(d.getId()).visitId(d.getVisitId()).chiefComplaint(d.getChiefComplaint()).symptoms(d.getSymptoms()).medicalHistory(d.getMedicalHistory()).physicalExamination(d.getPhysicalExamination()).clinicalProgress(d.getClinicalProgress()).treatmentPlan(d.getTreatmentPlan()).doctorInstructions(d.getDoctorInstructions()).conclusion(d.getConclusion()).status(d.getStatus()).lockedAt(d.getLockedAt()).lockedBy(d.getLockedBy()).createdBy(d.getCreatedBy()).createdAt(d.getCreatedAt()).updatedBy(d.getUpdatedBy()).updatedAt(d.getUpdatedAt()).build();
    }
}
