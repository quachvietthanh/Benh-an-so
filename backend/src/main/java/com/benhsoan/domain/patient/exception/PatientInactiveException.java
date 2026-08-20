package com.benhsoan.domain.patient.exception;


public class PatientInactiveException extends PatientException {

    public PatientInactiveException() {
        super(
                "Patient has been deactivated."
        );
    }

}
