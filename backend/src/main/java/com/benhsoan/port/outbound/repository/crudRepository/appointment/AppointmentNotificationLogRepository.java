package com.benhsoan.port.outbound.repository.crudRepository.appointment;

import java.util.UUID;

import com.benhsoan.domain.appointment.notification.AppointmentNotificationLog;
import com.benhsoan.port.outbound.repository.BaseRepository;

public interface AppointmentNotificationLogRepository
        extends BaseRepository<AppointmentNotificationLog, UUID> {

    boolean existsSentReminderByAppointmentId(UUID appointmentId);
}
