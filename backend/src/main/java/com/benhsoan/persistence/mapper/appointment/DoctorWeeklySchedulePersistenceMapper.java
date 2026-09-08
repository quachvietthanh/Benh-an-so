package com.benhsoan.persistence.mapper.appointment;

import java.time.DayOfWeek;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.appointment.DoctorWeeklySchedule;
import com.benhsoan.persistence.entity.appointment.DoctorWeeklyScheduleEntity;

@Component
public class DoctorWeeklySchedulePersistenceMapper {

    public DoctorWeeklySchedule toDomain(DoctorWeeklyScheduleEntity entity) {
        if (entity == null) {
            return null;
        }
        return DoctorWeeklySchedule.restore(
                entity.getId(),
                entity.getDoctorId(),
                DayOfWeek.of(entity.getDayOfWeek()),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public DoctorWeeklyScheduleEntity toEntity(DoctorWeeklySchedule domain) {
        if (domain == null) {
            return null;
        }
        return DoctorWeeklyScheduleEntity.builder()
                .id(domain.getId())
                .doctorId(domain.getDoctorId())
                .dayOfWeek(domain.getDayOfWeek().getValue())
                .startTime(domain.getStartTime())
                .endTime(domain.getEndTime())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

}
