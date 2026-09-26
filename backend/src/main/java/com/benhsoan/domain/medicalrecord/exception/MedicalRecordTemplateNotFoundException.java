package com.benhsoan.domain.medicalrecord.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class MedicalRecordTemplateNotFoundException extends MedicalRecordTemplateException {

    public MedicalRecordTemplateNotFoundException(UUID templateId) {
        super(DomainErrorCode.MEDICAL_RECORD_TEMPLATE_NOT_FOUND,
                "Medical record template does not exist: " + templateId);
    }
}
