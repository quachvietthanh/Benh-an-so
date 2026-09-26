package com.benhsoan.persistence.mapper.prescription;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.prescription.PrescriptionInterconnectionLog;
import com.benhsoan.persistence.entity.prescription.PrescriptionInterconnectionLogEntity;

@Component
public class PrescriptionInterconnectionLogPersistenceMapper {

    public PrescriptionInterconnectionLog toDomain(PrescriptionInterconnectionLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return PrescriptionInterconnectionLog.restore(
                entity.getId(), entity.getPrescriptionId(), entity.getAttemptNumber(),
                entity.getAttemptType(), entity.getOutcome(), entity.getRequestPayload(),
                entity.getResponsePayload(), entity.getReceiptCode(), entity.getFailureReason(),
                entity.getAttemptedBy(), entity.getStartedAt(), entity.getCompletedAt()
        );
    }

    public PrescriptionInterconnectionLogEntity toEntity(PrescriptionInterconnectionLog domain) {
        if (domain == null) {
            return null;
        }
        return PrescriptionInterconnectionLogEntity.builder()
                .id(domain.getId())
                .prescriptionId(domain.getPrescriptionId())
                .attemptNumber(domain.getAttemptNumber())
                .attemptType(domain.getAttemptType())
                .outcome(domain.getOutcome())
                .requestPayload(domain.getRequestPayload())
                .responsePayload(domain.getResponsePayload())
                .receiptCode(domain.getReceiptCode())
                .failureReason(domain.getFailureReason())
                .attemptedBy(domain.getAttemptedBy())
                .startedAt(domain.getStartedAt())
                .completedAt(domain.getCompletedAt())
                .build();
    }
}
