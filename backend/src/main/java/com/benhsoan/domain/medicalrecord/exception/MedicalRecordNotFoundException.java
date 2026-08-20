package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class MedicalRecordNotFoundException extends MedicalRecordException {

    public MedicalRecordNotFoundException(UUID medicalRecordId) {
        super(DomainErrorCode.MEDICAL_RECORD_NOT_FOUND, "Medical record not found: " + medicalRecordId);
    }
}
