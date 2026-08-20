package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class MedicalRecordNotLockedException extends MedicalRecordException {

    public MedicalRecordNotLockedException() {
        super(DomainErrorCode.MEDICAL_RECORD_NOT_LOCKED, "Only locked medical records can be amended.");
    }
}
