package com.benhsoan.domain.druginteraction.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public abstract class DrugInteractionException extends DomainException {

    protected DrugInteractionException(
            HttpStatus status,
            String message
    ) {
        super(status, message);
    }
}
