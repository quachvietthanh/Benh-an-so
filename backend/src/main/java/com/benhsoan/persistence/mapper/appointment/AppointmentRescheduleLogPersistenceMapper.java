package com.benhsoan.persistence.mapper.appointment;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.appointment.AppointmentRescheduleLog;
import com.benhsoan.persistence.entity.appointment.AppointmentRescheduleLogEntity;

@Component
public class AppointmentRescheduleLogPersistenceMapper {

    public AppointmentRescheduleLog toDomain(AppointmentRescheduleLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return AppointmentRescheduleLog.restore(
                entity.getId(),
                entity.getAppointmentId(),
                entity.getOldDoctorId(),
                entity.getNewDoctorId(),
                entity.getOldStartTime(),
                entity.getOldEndTime(),
                entity.getNewStartTime(),
                entity.getNewEndTime(),
                entity.getReason(),
                entity.getRescheduledBy(),
                entity.getRescheduledAt()
        );
    }

    public AppointmentRescheduleLogEntity toEntity(AppointmentRescheduleLog domain) {
        if (domain == null) {
            return null;
        }
        return AppointmentRescheduleLogEntity.builder()
                .id(domain.getId())
                .appointmentId(domain.getAppointmentId())
                .oldDoctorId(domain.getOldDoctorId())
                .newDoctorId(domain.getNewDoctorId())
                .oldStartTime(domain.getOldStartTime())
                .oldEndTime(domain.getOldEndTime())
                .newStartTime(domain.getNewStartTime())
                .newEndTime(domain.getNewEndTime())
                .reason(domain.getReason())
                .rescheduledBy(domain.getRescheduledBy())
                .rescheduledAt(domain.getRescheduledAt())
                .build();
    }
}
