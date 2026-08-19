package com.benhsoan.domain.carelog.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public abstract class CareLogException extends DomainException {

    protected CareLogException(HttpStatus status, String message) {
        super(status, message);
    }
}
