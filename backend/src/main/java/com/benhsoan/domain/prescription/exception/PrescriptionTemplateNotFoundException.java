package com.benhsoan.domain.prescription.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PrescriptionTemplateNotFoundException extends PrescriptionTemplateException {

    public PrescriptionTemplateNotFoundException(UUID templateId) {
        super(DomainErrorCode.PRESCRIPTION_TEMPLATE_NOT_FOUND,
                "Prescription template not found: " + templateId);
    }
}
