package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

public record DoctorRoomAssignmentResult(UUID id, UUID doctorId, UUID roomId, UUID assignedBy, Instant assignedAt) {
}
