package com.benhsoan.domain.medicalrecord.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class MedicalRecordAlreadyExistsForVisitException extends MedicalRecordException {

    public MedicalRecordAlreadyExistsForVisitException(UUID visitId) {
        super(HttpStatus.CONFLICT, "Medical record already exists for visit: " + visitId);
    }
}
