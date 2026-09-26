package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class MedicalRecordAccessDeniedException extends MedicalRecordException {

    public MedicalRecordAccessDeniedException() {
        super(DomainErrorCode.MEDICAL_RECORD_ACCESS_DENIED, "You do not have permission to view medical history.");
    }

    public MedicalRecordAccessDeniedException(String message) {
        super(DomainErrorCode.MEDICAL_RECORD_ACCESS_DENIED, message);
    }
}
