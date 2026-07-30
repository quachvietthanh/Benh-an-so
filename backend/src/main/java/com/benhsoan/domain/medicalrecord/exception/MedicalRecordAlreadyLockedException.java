package com.benhsoan.domain.medicalrecord.exception;

import org.springframework.http.HttpStatus;

public class MedicalRecordAlreadyLockedException extends MedicalRecordException {

    public MedicalRecordAlreadyLockedException() {
        super(HttpStatus.CONFLICT, "Medical record is already locked.");
    }
}
