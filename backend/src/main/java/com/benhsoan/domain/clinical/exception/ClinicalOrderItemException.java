package com.benhsoan.domain.clinical.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public abstract class ClinicalOrderItemException extends ClinicalException {

    protected ClinicalOrderItemException(DomainErrorCode code, String message) {
        super(code, message);
    }
}
