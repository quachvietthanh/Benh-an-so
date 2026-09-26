package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class MedicalRecordTemplateChangeWithContentException extends MedicalRecordTemplateException {

    public MedicalRecordTemplateChangeWithContentException() {
        super(DomainErrorCode.MEDICAL_RECORD_TEMPLATE_CHANGE_WITH_CONTENT,
                "A medical record template cannot be changed after clinical content has been entered.");
    }
}
