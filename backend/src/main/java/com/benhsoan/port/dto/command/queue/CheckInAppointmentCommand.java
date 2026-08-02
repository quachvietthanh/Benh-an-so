package com.benhsoan.port.dto.command.queue;

import java.util.UUID;

public record CheckInAppointmentCommand(UUID appointmentId) {
}
