package com.benhsoan.persistence.mapper.appointment;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.appointment.notification.AppointmentNotificationLog;
import com.benhsoan.persistence.entity.appointment.AppointmentNotificationLogEntity;

@Component
public class AppointmentNotificationLogPersistenceMapper {

    public AppointmentNotificationLog toDomain(AppointmentNotificationLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return AppointmentNotificationLog.restore(entity.getId(), entity.getAppointmentId(),
                entity.getPatientId(), entity.getNotificationType(), entity.getChannel(),
                entity.getContent(), entity.getStatus(), entity.getAttemptedAt(), entity.getSentAt(),
                entity.getFailureReason(), entity.getCreatedAt());
    }

    public AppointmentNotificationLogEntity toEntity(AppointmentNotificationLog domain) {
        if (domain == null) {
            return null;
        }
        return AppointmentNotificationLogEntity.builder()
                .id(domain.getId())
                .appointmentId(domain.getAppointmentId())
                .patientId(domain.getPatientId())
                .notificationType(domain.getNotificationType())
                .channel(domain.getChannel())
                .content(domain.getContent())
                .status(domain.getStatus())
                .attemptedAt(domain.getAttemptedAt())
                .sentAt(domain.getSentAt())
                .failureReason(domain.getFailureReason())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
