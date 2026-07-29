package com.benhsoan.persistence.jpaRepository.appointment;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.benhsoan.domain.appointment.notification.enums.NotificationStatus;
import com.benhsoan.domain.appointment.notification.enums.NotificationType;
import com.benhsoan.persistence.entity.appointment.AppointmentNotificationLogEntity;

public interface JpaAppointmentNotificationLogRepository
        extends JpaRepository<AppointmentNotificationLogEntity, UUID> {

    boolean existsByAppointmentIdAndNotificationTypeAndStatus(
            UUID appointmentId,
            NotificationType notificationType,
            NotificationStatus status
    );
}
