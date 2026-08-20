package com.benhsoan.domain.druginteraction.exception;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class DrugInteractionException extends DomainException {

    protected DrugInteractionException(
            String message
    ) {
        super(message);
    }
}
