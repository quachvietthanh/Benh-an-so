package com.benhsoan.domain.patient.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public class PatientNotFoundException extends DomainException {

    public PatientNotFoundException(UUID patientId) {
        super(
                HttpStatus.NOT_FOUND,
                "Patient not found: " + patientId
        );
    }

    public PatientNotFoundException(String patientCode) {
        super(
                HttpStatus.NOT_FOUND,
                "Patient not found: " + patientCode
        );
    }
}
