package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class MedicalRecordTemplateInactiveException extends MedicalRecordTemplateException {

    public MedicalRecordTemplateInactiveException() {
        super(DomainErrorCode.MEDICAL_RECORD_TEMPLATE_INACTIVE,
                "Inactive medical record template cannot be default.");
    }
}
