package com.benhsoan.domain.queue.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public class DoctorRoomAssignmentConflictException extends DomainException {

    public DoctorRoomAssignmentConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
