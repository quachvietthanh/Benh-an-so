package com.benhsoan.domain.patient.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PatientIdentityConflictException extends PatientException {

    public PatientIdentityConflictException(String reason) {
        super(DomainErrorCode.PATIENT_IDENTITY_CONFLICT,
                "Từ chối gộp do mâu thuẫn về danh tính: " + reason);
    }
}
