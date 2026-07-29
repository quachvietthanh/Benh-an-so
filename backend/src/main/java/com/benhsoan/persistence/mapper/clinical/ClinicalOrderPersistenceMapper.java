package com.benhsoan.persistence.mapper.clinical;

import org.springframework.stereotype.Component;
import com.benhsoan.domain.clinical.ClinicalOrder;
import com.benhsoan.persistence.entity.clinical.ClinicalOrderEntity;

@Component
public class ClinicalOrderPersistenceMapper {

    public ClinicalOrder toDomain(ClinicalOrderEntity e) {
        return e == null ? null : ClinicalOrder.restore(e.getId(), e.getOrderCode(), e.getVisitId(), e.getMedicalRecordId(), e.getPatientId(), e.getOrderedBy(), e.getClinicalReason(), e.getStatus(), e.getOrderedAt(), e.getCompletedAt(), e.getCreatedAt(), e.getUpdatedAt());
    }

    public ClinicalOrderEntity toEntity(ClinicalOrder d) {
        return d == null ? null : ClinicalOrderEntity.builder().id(d.getId()).orderCode(d.getOrderCode()).visitId(d.getVisitId()).medicalRecordId(d.getMedicalRecordId()).patientId(d.getPatientId()).orderedBy(d.getOrderedBy()).clinicalReason(d.getClinicalReason()).status(d.getStatus()).orderedAt(d.getOrderedAt()).completedAt(d.getCompletedAt()).createdAt(d.getCreatedAt()).updatedAt(d.getUpdatedAt()).build();
    }
}
