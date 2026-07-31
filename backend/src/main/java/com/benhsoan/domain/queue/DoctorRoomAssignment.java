package com.benhsoan.domain.queue;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;

@Getter
public class DoctorRoomAssignment {
    private final UUID id;
    private final UUID doctorId;
    private final UUID roomId;
    private final UUID assignedBy;
    private final Instant assignedAt;

    private DoctorRoomAssignment(UUID id, UUID doctorId, UUID roomId, UUID assignedBy, Instant assignedAt) {
        this.id = id;
        this.doctorId = doctorId;
        this.roomId = roomId;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
    }

    public static DoctorRoomAssignment restore(UUID id, UUID doctorId, UUID roomId, UUID assignedBy, Instant assignedAt) {
        return new DoctorRoomAssignment(id, doctorId, roomId, assignedBy, assignedAt);
    }
}
