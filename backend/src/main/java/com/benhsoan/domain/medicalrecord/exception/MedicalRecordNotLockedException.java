package com.benhsoan.domain.medicalrecord.exception;

import org.springframework.http.HttpStatus;

public class MedicalRecordNotLockedException extends MedicalRecordException {

    public MedicalRecordNotLockedException() {
        super(HttpStatus.CONFLICT, "Only locked medical records can be amended.");
    }
}
