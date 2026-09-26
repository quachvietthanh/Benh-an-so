package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class MedicalRecordTemplateNameDuplicateException extends MedicalRecordTemplateException {

    public MedicalRecordTemplateNameDuplicateException(String name) {
        super(DomainErrorCode.MEDICAL_RECORD_TEMPLATE_NAME_DUPLICATE,
                "Medical record template name already exists in the specialty: " + name);
    }
}
