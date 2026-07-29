package com.benhsoan.port.inbound.appointment;

import java.util.UUID;

import com.benhsoan.port.dto.result.AppointmentReminderResult;

public interface SendAppointmentReminderManuallyUseCase {

    AppointmentReminderResult sendManually(UUID appointmentId);
}
