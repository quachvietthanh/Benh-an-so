package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class MedicalRecordAlreadyLockedException extends MedicalRecordException {

    public MedicalRecordAlreadyLockedException() {
        super(DomainErrorCode.MEDICAL_RECORD_LOCKED, "Medical record is already locked.");
    }
}
