package com.benhsoan.persistence.mapper.carelog;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.carelog.PostCareLog;
import com.benhsoan.persistence.entity.carelog.PostCareLogEntity;

@Component
public class PostCareLogPersistenceMapper {

    public PostCareLog toDomain(PostCareLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return PostCareLog.restore(
                entity.getId(),
                entity.getPatientId(),
                entity.getReminderId(),
                entity.getVisitId(),
                entity.getContactChannel(),
                entity.getContactedAt(),
                entity.getPatientCondition(),
                entity.getCareNotes(),
                entity.getContactOutcome(),
                entity.getPerformedBy(),
                entity.getCreatedAt()
        );
    }

    public PostCareLogEntity toEntity(PostCareLog domain) {
        if (domain == null) {
            return null;
        }
        return PostCareLogEntity.builder()
                .id(domain.getId())
                .patientId(domain.getPatientId())
                .reminderId(domain.getReminderId())
                .visitId(domain.getVisitId())
                .contactChannel(domain.getContactChannel())
                .contactedAt(domain.getContactedAt())
                .patientCondition(domain.getPatientCondition())
                .careNotes(domain.getCareNotes())
                .contactOutcome(domain.getContactOutcome())
                .performedBy(domain.getPerformedBy())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
