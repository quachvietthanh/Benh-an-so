package com.benhsoan.domain.patient.exception;

import java.util.UUID;


public class PatientNotFoundException extends PatientException {

    public PatientNotFoundException(UUID patientId) {
        super(
                "Patient not found: " + patientId
        );
    }

    public PatientNotFoundException(String patientCode) {
        super(
                "Patient not found: " + patientCode
        );
    }
}
