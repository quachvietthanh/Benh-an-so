package com.benhsoan.domain.queue.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;

public class DoctorNotAssignedToRoomException extends QueueException {

    public DoctorNotAssignedToRoomException(UUID doctorId) {
        super(DomainErrorCode.DOCTOR_NOT_ASSIGNED_TO_ROOM, "Doctor does not have an active room assignment: " + doctorId);
    }
}
