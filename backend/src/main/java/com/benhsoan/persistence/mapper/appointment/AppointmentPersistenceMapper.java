package com.benhsoan.persistence.mapper.appointment;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.appointment.Appointment;
import com.benhsoan.persistence.entity.appointment.AppointmentEntity;

@Component
public class AppointmentPersistenceMapper {

    public Appointment toDomain(AppointmentEntity entity) {
        if (entity == null) {
            return null;
        }

        return Appointment.restore(
                entity.getId(),
                entity.getAppointmentCode(),
                entity.getPatientId(),
                entity.getDoctorId(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getStatus(),
                entity.getReason(),
                entity.getCancelReason(),
                entity.getCheckedInAt(),
                entity.getCompletedAt(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getBookingChannel()
        );
    }

    public AppointmentEntity toEntity(Appointment domain) {
        if (domain == null) {
            return null;
        }

        return AppointmentEntity.builder()
                .id(domain.getId())
                .appointmentCode(domain.getAppointmentCode())
                .patientId(domain.getPatientId())
                .doctorId(domain.getDoctorId())
                .startTime(domain.getStartTime())
                .endTime(domain.getEndTime())
                .status(domain.getStatus())
                .reason(domain.getReason())
                .cancelReason(domain.getCancelReason())
                .checkedInAt(domain.getCheckedInAt())
                .completedAt(domain.getCompletedAt())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .bookingChannel(domain.getBookingChannel())
                .build();
    }

}