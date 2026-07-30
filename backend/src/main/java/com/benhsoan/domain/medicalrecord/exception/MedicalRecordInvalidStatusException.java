package com.benhsoan.domain.medicalrecord.exception;

import org.springframework.http.HttpStatus;

public class MedicalRecordInvalidStatusException extends MedicalRecordException {

    public MedicalRecordInvalidStatusException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
