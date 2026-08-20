package com.benhsoan.domain.visit.exception;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class VisitException extends DomainException {

    protected VisitException(String message) {
        super(message);
    }
}
