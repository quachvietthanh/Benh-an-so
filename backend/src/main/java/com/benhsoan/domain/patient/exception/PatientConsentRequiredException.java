package com.benhsoan.domain.patient.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

/**
 * Thrown when attempting to create/save a patient record without recorded personal data processing consent (QTN-24).
 */
public class PatientConsentRequiredException extends PatientException {

    public PatientConsentRequiredException() {
        super(DomainErrorCode.PATIENT_CONSENT_REQUIRED,
                "Phải có phiếu đồng ý trước khi xử lý dữ liệu cá nhân (QTN-24).");
    }

    public PatientConsentRequiredException(String message) {
        super(DomainErrorCode.PATIENT_CONSENT_REQUIRED, message);
    }
}
