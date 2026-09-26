package com.benhsoan.domain.medicalrecord.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class MedicalRecordUnauthorizedSignerException extends MedicalRecordException {

    public MedicalRecordUnauthorizedSignerException(UUID medicalRecordId, UUID doctorId) {
        super(DomainErrorCode.MEDICAL_RECORD_UNAUTHORIZED_SIGNER,
                "Only the doctor in charge (" + doctorId + ") can sign medical record: " + medicalRecordId);
    }
}
