package com.benhsoan.domain.medicalrecord.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public abstract class MedicalRecordException extends DomainException {

    protected MedicalRecordException(HttpStatus status, String message) {
        super(status, message);
    }
}
