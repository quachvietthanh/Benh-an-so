package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class MedicalRecordInvalidVisitException extends MedicalRecordException {

    public MedicalRecordInvalidVisitException(UUID visitId) {
        super(DomainErrorCode.MEDICAL_RECORD_INVALID_VISIT, "Medical record operation is not allowed for inactive visit: " + visitId);
    }
}
