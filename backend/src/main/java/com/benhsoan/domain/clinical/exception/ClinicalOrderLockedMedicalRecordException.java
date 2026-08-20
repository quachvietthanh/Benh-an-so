package com.benhsoan.domain.clinical.exception;


public class ClinicalOrderLockedMedicalRecordException extends ClinicalOrderException {

    public ClinicalOrderLockedMedicalRecordException() {
        super("Clinical orders cannot be created for a locked medical record.");
    }
}
