package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class MedicalRecordInvalidStatusException extends MedicalRecordException {

    public MedicalRecordInvalidStatusException(String message) {
        super(DomainErrorCode.MEDICAL_RECORD_INVALID_STATUS, message);
    }
}
