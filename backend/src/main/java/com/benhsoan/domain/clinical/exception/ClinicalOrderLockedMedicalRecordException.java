package com.benhsoan.domain.clinical.exception;

import org.springframework.http.HttpStatus;

public class ClinicalOrderLockedMedicalRecordException extends ClinicalOrderException {

    public ClinicalOrderLockedMedicalRecordException() {
        super(HttpStatus.CONFLICT, "Clinical orders cannot be created for a locked medical record.");
    }
}
