package com.benhsoan.domain.patient.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class PatientInactiveException extends PatientException {

    public PatientInactiveException() {
        super(DomainErrorCode.PATIENT_INACTIVE,
                "Patient has been deactivated."
        );
    }

}
