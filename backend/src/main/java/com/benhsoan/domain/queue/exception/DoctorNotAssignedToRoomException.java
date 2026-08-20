package com.benhsoan.domain.queue.exception;

import java.util.UUID;

public class DoctorNotAssignedToRoomException extends QueueException {

    public DoctorNotAssignedToRoomException(UUID doctorId) {
        super("Doctor does not have an active room assignment: " + doctorId);
    }
}
