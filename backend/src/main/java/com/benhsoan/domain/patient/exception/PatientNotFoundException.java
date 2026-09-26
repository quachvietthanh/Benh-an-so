package com.benhsoan.domain.patient.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class PatientNotFoundException extends PatientException {

    public PatientNotFoundException(UUID patientId) {
        super(DomainErrorCode.PATIENT_NOT_FOUND,
                "Patient not found: " + patientId
        );
    }

    public PatientNotFoundException(String patientCode) {
        super(DomainErrorCode.PATIENT_NOT_FOUND,
                "Patient not found: " + patientCode
        );
    }
}
