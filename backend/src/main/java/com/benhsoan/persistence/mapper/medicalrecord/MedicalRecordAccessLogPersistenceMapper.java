package com.benhsoan.persistence.mapper.medicalrecord;

import org.springframework.stereotype.Component;
import com.benhsoan.domain.medicalrecord.MedicalRecordAccessLog;
import com.benhsoan.persistence.entity.medicalrecord.MedicalRecordAccessLogEntity;

@Component
public class MedicalRecordAccessLogPersistenceMapper {

    public MedicalRecordAccessLog toDomain(MedicalRecordAccessLogEntity e) {
        return e == null ? null : MedicalRecordAccessLog.restore(e.getId(), e.getPatientId(), e.getVisitId(), e.getMedicalRecordId(), e.getAccessedBy(), e.getAction(), e.getDetail(), null, e.getAccessedAt());
    }

    public MedicalRecordAccessLogEntity toEntity(MedicalRecordAccessLog d) {
        return d == null ? null : MedicalRecordAccessLogEntity.builder().id(d.getId()).patientId(d.getPatientId()).visitId(d.getVisitId()).medicalRecordId(d.getMedicalRecordId()).accessedBy(d.getAccessedBy()).action(d.getAction()).detail(d.getDetail()).ipAddress(null).accessedAt(d.getAccessedAt()).build();
    }
}
