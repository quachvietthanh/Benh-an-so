package com.benhsoan.persistence.mapper.clinical;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.clinical.ClinicalResultHistory;
import com.benhsoan.persistence.entity.clinical.ClinicalResultHistoryEntity;

@Component
public class ClinicalResultHistoryPersistenceMapper {

    public ClinicalResultHistory toDomain(ClinicalResultHistoryEntity e) {
        return e == null ? null : ClinicalResultHistory.restore(e.getId(), e.getClinicalResultId(), e.getOldResultType(), e.getNewResultType(), e.getOldNumericValue(), e.getNewNumericValue(), e.getOldTextValue(), e.getNewTextValue(), e.getOldUnit(), e.getNewUnit(), e.getOldReferenceRange(), e.getNewReferenceRange(), e.getOldAbnormalFlag(), e.getNewAbnormalFlag(), e.getOldConclusion(), e.getNewConclusion(), e.getOldStatus(), e.getNewStatus(), e.getChangeReason(), e.getChangedBy(), e.getChangedAt());
    }

    public ClinicalResultHistoryEntity toEntity(ClinicalResultHistory d) {
        return d == null ? null : ClinicalResultHistoryEntity.builder().id(d.getId()).clinicalResultId(d.getClinicalResultId()).oldResultType(d.getOldResultType()).newResultType(d.getNewResultType()).oldNumericValue(d.getOldNumericValue()).newNumericValue(d.getNewNumericValue()).oldTextValue(d.getOldTextValue()).newTextValue(d.getNewTextValue()).oldUnit(d.getOldUnit()).newUnit(d.getNewUnit()).oldReferenceRange(d.getOldReferenceRange()).newReferenceRange(d.getNewReferenceRange()).oldAbnormalFlag(d.getOldAbnormalFlag()).newAbnormalFlag(d.getNewAbnormalFlag()).oldConclusion(d.getOldConclusion()).newConclusion(d.getNewConclusion()).oldStatus(d.getOldStatus()).newStatus(d.getNewStatus()).changeReason(d.getChangeReason()).changedBy(d.getChangedBy()).changedAt(d.getChangedAt()).build();
    }
}
