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

        return MedicalQueue.restore(entity.getId(), entity.getDoctorId(), entity.getRoomId(), entity.getQueueDate(),
                entity.getStatus(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public MedicalQueueEntity toEntity(MedicalQueue domain) {

        if (domain == null) {
            return null;
        }

        return MedicalQueueEntity.builder()
                .id(domain.getId())
                .doctorId(domain.getDoctorId())
                .roomId(domain.getRoomId())
                .queueDate(domain.getQueueDate())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
