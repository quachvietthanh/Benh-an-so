package com.benhsoan.persistence.mapper.appointment;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.appointment.DoctorSchedule;
import com.benhsoan.persistence.entity.appointment.DoctorScheduleEntity;

@Component
public class DoctorSchedulePersistenceMapper {

    public DoctorSchedule toDomain(DoctorScheduleEntity entity) {
        if (entity == null) {
            return null;
        }
        return DoctorSchedule.restore(
                entity.getId(),
                entity.getDoctorId(),
                entity.getScheduleDate(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public DoctorScheduleEntity toEntity(DoctorSchedule domain) {
        if (domain == null) {
            return null;
        }
        return DoctorScheduleEntity.builder()
                .id(domain.getId())
                .doctorId(domain.getDoctorId())
                .scheduleDate(domain.getScheduleDate())
                .startTime(domain.getStartTime())
                .endTime(domain.getEndTime())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

}
