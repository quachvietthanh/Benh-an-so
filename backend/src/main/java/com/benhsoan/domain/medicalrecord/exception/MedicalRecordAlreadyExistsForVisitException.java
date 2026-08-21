package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class MedicalRecordAlreadyExistsForVisitException extends MedicalRecordException {

    public MedicalRecordAlreadyExistsForVisitException(UUID visitId) {
        super(DomainErrorCode.MEDICAL_RECORD_ALREADY_EXISTS_FOR_VISIT, "Medical record already exists for visit: " + visitId);
    }
}
