package com.benhsoan.port.dto.command.queue;

import java.util.UUID;

public record CheckInWalkInCommand(UUID patientId, UUID doctorId, String reason, String note, UUID specialtyId) {
    public CheckInWalkInCommand(UUID patientId, UUID doctorId, String reason, String note) {
        this(patientId, doctorId, reason, note, null);
    }
}
