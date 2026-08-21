package com.benhsoan.domain.patient.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class PatientAlreadyExistsException extends PatientException {

    public PatientAlreadyExistsException(String field) {
        super(DomainErrorCode.PATIENT_ALREADY_EXISTS,
                "Patient already exists with " + field + "."
        );
    }

}
