package com.benhsoan.persistence.mapper.clinical;

import org.springframework.stereotype.Component;
import com.benhsoan.domain.clinical.ClinicalResult;
import com.benhsoan.persistence.entity.clinical.ClinicalResultEntity;

@Component
public class ClinicalResultPersistenceMapper {

    public ClinicalResult toDomain(ClinicalResultEntity e) {
        return e == null ? null : ClinicalResult.restore(e.getId(), e.getClinicalOrderItemId(), e.getVisitId(), e.getResultType(), e.getNumericValue(), e.getTextValue(), e.getUnit(), e.getReferenceRange(), e.getAbnormalFlag(), e.getConclusion(), e.getStatus(), e.getEnteredBy(), e.getEnteredAt(), e.getUpdatedBy(), e.getUpdatedAt());
    }

    public ClinicalResultEntity toEntity(ClinicalResult d) {
        return d == null ? null : ClinicalResultEntity.builder().id(d.getId()).clinicalOrderItemId(d.getClinicalOrderItemId()).visitId(d.getVisitId()).resultType(d.getResultType()).numericValue(d.getNumericValue()).textValue(d.getTextValue()).unit(d.getUnit()).referenceRange(d.getReferenceRange()).abnormalFlag(d.getAbnormalFlag()).conclusion(d.getConclusion()).status(d.getStatus()).enteredBy(d.getEnteredBy()).enteredAt(d.getEnteredAt()).updatedBy(d.getUpdatedBy()).updatedAt(d.getUpdatedAt()).build();
    }
}
