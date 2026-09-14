package com.benhsoan.port.outbound.repository.appointment;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.appointment.AppointmentRescheduleLog;

public interface AppointmentRescheduleLogRepository {

    AppointmentRescheduleLog save(AppointmentRescheduleLog log);

    List<AppointmentRescheduleLog> findByAppointmentId(UUID appointmentId);

}
