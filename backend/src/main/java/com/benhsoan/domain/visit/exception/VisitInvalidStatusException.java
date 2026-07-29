package com.benhsoan.domain.visit.exception;

import org.springframework.http.HttpStatus;

public class VisitInvalidStatusException extends VisitException {

    public VisitInvalidStatusException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
