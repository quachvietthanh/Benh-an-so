package com.benhsoan.domain.clinical.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class ClinicalOrderLockedMedicalRecordException extends ClinicalOrderException {

    public ClinicalOrderLockedMedicalRecordException() {
        super(DomainErrorCode.CLINICAL_ORDER_LOCKED_MEDICAL_RECORD, "Clinical orders cannot be created for a locked medical record.");
    }
}
