package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class MedicalRecordTemplateLastActiveException extends MedicalRecordTemplateException {

    public MedicalRecordTemplateLastActiveException() {
        super(DomainErrorCode.MEDICAL_RECORD_TEMPLATE_LAST_ACTIVE,
                "The last active medical record template in a specialty cannot be deactivated.");
    }
}
