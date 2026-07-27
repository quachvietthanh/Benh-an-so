package com.benhsoan.persistence.mapper.queue;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.queue.MedicalQueue;
import com.benhsoan.persistence.entity.queue.MedicalQueueEntity;

@Component
public class MedicalQueuePersistenceMapper {

    public MedicalQueue toDomain(MedicalQueueEntity entity) {

        if (entity == null) {
            return null;
        }

        return MedicalQueue.restore(
                entity.getId(),
                entity.getPatientId(),
                entity.getDoctorId(),
                entity.getRoomNumber(),
                entity.getQueueNumber(),
                entity.getStatus(),
                entity.getPriorityLevel(),
                entity.getNotes(),
                entity.getCheckedInAt(),
                entity.getCalledAt(),
                entity.getStartedAt(),
                entity.getWaitingForResultAt(),
                entity.getCompletedAt(),
                entity.getCancelledAt(),
                entity.getCancelReason(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public MedicalQueueEntity toEntity(MedicalQueue domain) {

        if (domain == null) {
            return null;
        }

        return MedicalQueueEntity.builder()
                .id(domain.getId())
                .patientId(domain.getPatientId())
                .doctorId(domain.getDoctorId())
                .roomNumber(domain.getRoomNumber())
                .queueNumber(domain.getQueueNumber())
                .status(domain.getStatus())
                .priorityLevel(domain.getPriorityLevel())
                .notes(domain.getNotes())
                .checkedInAt(domain.getCheckedInAt())
                .calledAt(domain.getCalledAt())
                .startedAt(domain.getStartedAt())
                .waitingForResultAt(domain.getWaitingForResultAt())
                .completedAt(domain.getCompletedAt())
                .cancelledAt(domain.getCancelledAt())
                .cancelReason(domain.getCancelReason())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
