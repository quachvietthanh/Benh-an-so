package com.benhsoan.domain.queue.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public class DoctorRoomAssignmentNotFoundException extends DomainException {

    public DoctorRoomAssignmentNotFoundException(UUID doctorId) {
        super(HttpStatus.CONFLICT, "Doctor does not have an active room assignment: " + doctorId);
    }
}
