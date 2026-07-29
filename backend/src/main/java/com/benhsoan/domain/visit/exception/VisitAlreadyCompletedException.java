package com.benhsoan.domain.visit.exception;

import org.springframework.http.HttpStatus;

public class VisitAlreadyCompletedException extends VisitException {

    public VisitAlreadyCompletedException() {
        super(HttpStatus.CONFLICT, "Visit has already been completed.");
    }
}
