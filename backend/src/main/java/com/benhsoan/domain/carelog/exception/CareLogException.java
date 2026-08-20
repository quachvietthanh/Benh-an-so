package com.benhsoan.domain.carelog.exception;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class CareLogException extends DomainException {

    protected CareLogException(String message) {
        super(message);
    }
}
