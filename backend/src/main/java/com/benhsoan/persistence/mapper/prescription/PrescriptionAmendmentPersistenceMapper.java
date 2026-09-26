package com.benhsoan.persistence.mapper.prescription;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.prescription.PrescriptionAmendment;
import com.benhsoan.persistence.entity.prescription.PrescriptionAmendmentEntity;

@Component
public class PrescriptionAmendmentPersistenceMapper {

    public PrescriptionAmendment toDomain(PrescriptionAmendmentEntity entity) {
        if (entity == null) {
            return null;
        }

        return PrescriptionAmendment.restore(
                entity.getId(),
                entity.getPrescriptionId(),
                entity.getChangeReason(),
                entity.getBeforeData(),
                entity.getAfterData(),
                entity.getAmendedBy(),
                entity.getAmendedAt()
        );
    }

    public PrescriptionAmendmentEntity toEntity(PrescriptionAmendment domain) {
        if (domain == null) {
            return null;
        }

        return PrescriptionAmendmentEntity.builder()
                .id(domain.getId())
                .prescriptionId(domain.getPrescriptionId())
                .changeReason(domain.getChangeReason())
                .beforeData(domain.getBeforeData())
                .afterData(domain.getAfterData())
                .amendedBy(domain.getAmendedBy())
                .amendedAt(domain.getAmendedAt())
                .build();
    }
}
