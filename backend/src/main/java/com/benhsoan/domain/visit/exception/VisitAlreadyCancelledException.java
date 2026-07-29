package com.benhsoan.domain.visit.exception;

import org.springframework.http.HttpStatus;

public class VisitAlreadyCancelledException extends VisitException {

    public VisitAlreadyCancelledException() {
        super(HttpStatus.CONFLICT, "Visit has already been cancelled.");
    }
}
