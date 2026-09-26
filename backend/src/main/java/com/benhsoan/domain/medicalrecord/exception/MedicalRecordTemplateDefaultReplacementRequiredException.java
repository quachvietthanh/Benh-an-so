package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class MedicalRecordTemplateDefaultReplacementRequiredException extends MedicalRecordTemplateException {

    public MedicalRecordTemplateDefaultReplacementRequiredException() {
        super(DomainErrorCode.MEDICAL_RECORD_TEMPLATE_DEFAULT_REPLACEMENT_REQUIRED,
                "Deactivating the default medical record template requires another active replacement.");
    }

    public MedicalRecordTemplateDefaultReplacementRequiredException(String message) {
        super(DomainErrorCode.MEDICAL_RECORD_TEMPLATE_DEFAULT_REPLACEMENT_REQUIRED, message);
    }
}
