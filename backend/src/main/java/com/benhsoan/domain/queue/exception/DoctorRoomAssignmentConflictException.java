package com.benhsoan.domain.queue.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class DoctorRoomAssignmentConflictException extends QueueException {

    public DoctorRoomAssignmentConflictException(String message) {
        super(DomainErrorCode.DOCTOR_ROOM_ASSIGNMENT_CONFLICT, message);
    }
}
