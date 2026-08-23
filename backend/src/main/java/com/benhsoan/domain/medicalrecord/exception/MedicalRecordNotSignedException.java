package com.benhsoan.domain.medicalrecord.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;
import com.benhsoan.domain.shared.exception.DomainException;

public class MedicalRecordNotSignedException extends DomainException {

    public MedicalRecordNotSignedException(UUID medicalRecordId) {
        super(
                DomainErrorCode.MEDICAL_RECORD_NOT_SIGNED,
                "Medical record with ID " + medicalRecordId + " must be signed before it can be locked."
        );
    }
}
