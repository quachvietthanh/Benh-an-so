package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class MedicalRecordTemplateSpecialtyMismatchException extends MedicalRecordTemplateException {

    public MedicalRecordTemplateSpecialtyMismatchException() {
        super(DomainErrorCode.MEDICAL_RECORD_TEMPLATE_SPECIALTY_MISMATCH,
                "The medical record template does not match the visit specialty or its allowed GENERAL fallback.");
    }
}
