package com.benhsoan.adapter.inbound.rest.response.queue;
import java.time.Instant;
import java.util.UUID;
public record DoctorRoomAssignmentResponse(UUID id, UUID doctorId, UUID roomId, UUID assignedBy, Instant assignedAt) { }
