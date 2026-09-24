package com.benhsoan.domain.prescription.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

public abstract class PrescriptionTemplateException extends DomainException {

    protected PrescriptionTemplateException(DomainErrorCode code, String message) {
        super(code, message);
    }
}
