package com.benhsoan.domain.druginteraction.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class DrugInteractionException extends DomainException {

    protected DrugInteractionException(
            DomainErrorCode code,
            String message
    ) {
        super(code, message);
    }
}
