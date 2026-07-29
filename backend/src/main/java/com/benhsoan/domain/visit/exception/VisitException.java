package com.benhsoan.domain.visit.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public abstract class VisitException extends DomainException {

    protected VisitException(HttpStatus status, String message) {
        super(status, message);
    }
}
