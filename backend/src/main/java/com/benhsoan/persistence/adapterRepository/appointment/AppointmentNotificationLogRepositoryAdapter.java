package com.benhsoan.persistence.adapterRepository.appointment;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.benhsoan.domain.appointment.notification.AppointmentNotificationLog;
import com.benhsoan.domain.appointment.notification.enums.NotificationStatus;
import com.benhsoan.domain.appointment.notification.enums.NotificationType;
import com.benhsoan.persistence.jpaRepository.appointment.JpaAppointmentNotificationLogRepository;
import com.benhsoan.persistence.mapper.appointment.AppointmentNotificationLogPersistenceMapper;
import com.benhsoan.port.outbound.repository.crudRepository.appointment.AppointmentNotificationLogRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AppointmentNotificationLogRepositoryAdapter
        implements AppointmentNotificationLogRepository {

    private final JpaAppointmentNotificationLogRepository jpaRepository;
    private final AppointmentNotificationLogPersistenceMapper mapper;

    @Override
    public Optional<AppointmentNotificationLog> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public AppointmentNotificationLog save(AppointmentNotificationLog notificationLog) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(notificationLog)));
    }

    @Override
    public void deleteById(UUID id) {
        if (id != null) {
            jpaRepository.deleteById(id);
        }
    }

    @Override
    public boolean existsSentReminderByAppointmentId(UUID appointmentId) {
        return jpaRepository.existsByAppointmentIdAndNotificationTypeAndStatus(appointmentId,
                NotificationType.APPOINTMENT_REMINDER, NotificationStatus.SENT);
    }
}
