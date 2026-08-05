package com.benhsoan.port.outbound.repository.appointment;

import java.util.UUID;

import com.benhsoan.domain.appointment.notification.AppointmentNotificationLog;
public interface AppointmentNotificationLogRepository {

    AppointmentNotificationLog save(AppointmentNotificationLog notificationLog);

    boolean existsSentReminderByAppointmentId(UUID appointmentId);
}
