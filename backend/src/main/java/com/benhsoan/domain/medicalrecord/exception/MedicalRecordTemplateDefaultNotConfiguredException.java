package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class MedicalRecordTemplateDefaultNotConfiguredException extends MedicalRecordTemplateException {

    public MedicalRecordTemplateDefaultNotConfiguredException() {
        super(DomainErrorCode.MEDICAL_RECORD_TEMPLATE_DEFAULT_NOT_CONFIGURED,
                "The GENERAL specialty must have exactly one active default medical record template.");
    }
}
