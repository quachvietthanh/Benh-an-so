package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

public abstract class DiagnosisCatalogException extends DomainException {

    protected DiagnosisCatalogException(DomainErrorCode code, String message) {
        super(code, message);
    }
}
