package com.benhsoan.domain.clinical.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class ClinicalException extends DomainException {

    protected ClinicalException(DomainErrorCode code, String message) {
        super(code, message);
    }
}
