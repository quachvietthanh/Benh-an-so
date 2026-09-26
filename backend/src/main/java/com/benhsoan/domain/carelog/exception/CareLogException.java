package com.benhsoan.domain.carelog.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class CareLogException extends DomainException {

    protected CareLogException(DomainErrorCode code, String message) {
        super(code, message);
    }
}
