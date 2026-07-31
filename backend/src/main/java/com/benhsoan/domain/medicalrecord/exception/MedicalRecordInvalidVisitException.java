package com.benhsoan.domain.medicalrecord.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class MedicalRecordInvalidVisitException extends MedicalRecordException {

    public MedicalRecordInvalidVisitException(UUID visitId) {
        super(HttpStatus.CONFLICT, "Medical record operation is not allowed for inactive visit: " + visitId);
    }
}
