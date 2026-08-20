package com.benhsoan.domain.patient.exception;


public class PatientAlreadyExistsException extends PatientException {

    public PatientAlreadyExistsException(String field) {
        super(
                "Patient already exists with " + field + "."
        );
    }

}
