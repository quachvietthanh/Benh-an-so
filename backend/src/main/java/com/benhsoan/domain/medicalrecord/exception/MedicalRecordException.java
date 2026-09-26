package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class MedicalRecordException extends DomainException {

    protected MedicalRecordException(DomainErrorCode code, String message) {
        super(code, message);
    }
}
