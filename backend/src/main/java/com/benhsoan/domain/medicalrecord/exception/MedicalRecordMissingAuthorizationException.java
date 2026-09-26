package com.benhsoan.domain.medicalrecord.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class MedicalRecordMissingAuthorizationException extends MedicalRecordException {

    public MedicalRecordMissingAuthorizationException(UUID medicalRecordId) {
        super(DomainErrorCode.MEDICAL_RECORD_MISSING_AUTHORIZATION,
                "An authorization document is required to issue a copy of medical record " + medicalRecordId
                        + " to an authorized representative.");
    }
}
