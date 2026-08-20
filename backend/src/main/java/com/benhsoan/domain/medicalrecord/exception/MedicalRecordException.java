package com.benhsoan.domain.medicalrecord.exception;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class MedicalRecordException extends DomainException {

    protected MedicalRecordException(String message) {
        super(message);
    }
}
