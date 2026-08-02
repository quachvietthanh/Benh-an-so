package com.benhsoan.port.dto.command.queue;

import java.util.UUID;

public record CheckInWalkInCommand(UUID patientId, UUID doctorId, String reason, String note) {
}
