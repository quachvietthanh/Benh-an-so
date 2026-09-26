package com.benhsoan.domain.medicalrecord.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class MedicalRecordUnauthorizedRecipientException extends MedicalRecordException {

    public MedicalRecordUnauthorizedRecipientException(UUID medicalRecordId, String recipientName) {
        super(DomainErrorCode.MEDICAL_RECORD_UNAUTHORIZED_RECIPIENT,
                "Recipient '" + recipientName + "' is not authorized to receive a copy of medical record "
                        + medicalRecordId + ".");
    }
}
