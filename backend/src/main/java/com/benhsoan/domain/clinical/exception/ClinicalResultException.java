package com.benhsoan.domain.clinical.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public abstract class ClinicalResultException extends ClinicalException {

    protected ClinicalResultException(DomainErrorCode code, String message) {
        super(code, message);
    }
}
