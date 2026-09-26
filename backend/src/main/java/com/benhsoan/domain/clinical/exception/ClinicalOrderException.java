package com.benhsoan.domain.clinical.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public abstract class ClinicalOrderException extends ClinicalException {

    protected ClinicalOrderException(DomainErrorCode code, String m) {
        super(code, m);
    }
}
