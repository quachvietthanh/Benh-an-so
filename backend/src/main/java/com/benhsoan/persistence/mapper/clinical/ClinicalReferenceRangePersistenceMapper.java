package com.benhsoan.persistence.mapper.clinical;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.clinical.ClinicalReferenceRange;
import com.benhsoan.persistence.entity.clinical.ClinicalReferenceRangeEntity;

@Component
public class ClinicalReferenceRangePersistenceMapper {

    public ClinicalReferenceRange toDomain(ClinicalReferenceRangeEntity e) {
        return e == null ? null : ClinicalReferenceRange.restore(
                e.getId(), e.getClinicalServiceId(), e.getGender(), e.getMinAge(), e.getMaxAge(),
                e.getLowerBound(), e.getUpperBound(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt()
        );
    }

    public ClinicalReferenceRangeEntity toEntity(ClinicalReferenceRange d) {
        return d == null ? null : ClinicalReferenceRangeEntity.builder()
                .id(d.getId())
                .clinicalServiceId(d.getClinicalServiceId())
                .gender(d.getGender())
                .minAge(d.getMinAge())
                .maxAge(d.getMaxAge())
                .lowerBound(d.getLowerBound())
                .upperBound(d.getUpperBound())
                .active(d.isActive())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }
}
