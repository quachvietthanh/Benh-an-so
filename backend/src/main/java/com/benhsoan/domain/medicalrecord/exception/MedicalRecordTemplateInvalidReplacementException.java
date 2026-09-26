package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class MedicalRecordTemplateInvalidReplacementException extends MedicalRecordTemplateException {

    public MedicalRecordTemplateInvalidReplacementException() {
        super(DomainErrorCode.MEDICAL_RECORD_TEMPLATE_INVALID_REPLACEMENT,
                "Default replacement must be another active template in the same specialty.");
    }
}
