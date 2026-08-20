package com.benhsoan.domain.medicalrecord.exception;


public class MedicalRecordAlreadyLockedException extends MedicalRecordException {

    public MedicalRecordAlreadyLockedException() {
        super("Medical record is already locked.");
    }
}
