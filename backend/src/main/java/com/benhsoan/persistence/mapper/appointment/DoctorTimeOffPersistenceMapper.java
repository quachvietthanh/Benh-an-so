package com.benhsoan.persistence.mapper.appointment;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.appointment.DoctorTimeOff;
import com.benhsoan.persistence.entity.appointment.DoctorTimeOffEntity;

@Component
public class DoctorTimeOffPersistenceMapper {

    public DoctorTimeOff toDomain(DoctorTimeOffEntity entity) {
        if (entity == null) {
            return null;
        }
        return DoctorTimeOff.restore(
                entity.getId(),
                entity.getDoctorId(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getReason(),
                entity.getStatus(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public DoctorTimeOffEntity toEntity(DoctorTimeOff domain) {
        if (domain == null) {
            return null;
        }
        return DoctorTimeOffEntity.builder()
                .id(domain.getId())
                .doctorId(domain.getDoctorId())
                .startTime(domain.getStartTime())
                .endTime(domain.getEndTime())
                .reason(domain.getReason())
                .status(domain.getStatus())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

}
