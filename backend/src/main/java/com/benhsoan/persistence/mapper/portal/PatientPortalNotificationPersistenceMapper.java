package com.benhsoan.persistence.mapper.portal;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.portal.notification.PatientPortalNotification;
import com.benhsoan.persistence.entity.portal.PatientPortalNotificationEntity;

@Component
public class PatientPortalNotificationPersistenceMapper {

    public PatientPortalNotification toDomain(PatientPortalNotificationEntity entity) {
        if (entity == null) {
            return null;
        }
        return PatientPortalNotification.restore(
                entity.getId(),
                entity.getPatientId(),
                entity.getType(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getReadAt(),
                entity.getCreatedAt(),
                entity.getAppointmentId(),
                entity.getRescheduleLogId(),
                entity.getClinicalResultId());
    }

    public PatientPortalNotificationEntity toEntity(PatientPortalNotification domain) {
        if (domain == null) {
            return null;
        }
        return PatientPortalNotificationEntity.builder()
                .id(domain.getId())
                .patientId(domain.getPatientId())
                .type(domain.getType())
                .title(domain.getTitle())
                .message(domain.getMessage())
                .readAt(domain.getReadAt())
                .createdAt(domain.getCreatedAt())
                .appointmentId(domain.getAppointmentId())
                .rescheduleLogId(domain.getRescheduleLogId())
                .clinicalResultId(domain.getClinicalResultId())
                .build();
    }
}
