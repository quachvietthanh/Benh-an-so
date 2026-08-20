package com.benhsoan.domain.medicalrecord.exception;


public class MedicalRecordNotLockedException extends MedicalRecordException {

    public MedicalRecordNotLockedException() {
        super("Only locked medical records can be amended.");
    }
}
