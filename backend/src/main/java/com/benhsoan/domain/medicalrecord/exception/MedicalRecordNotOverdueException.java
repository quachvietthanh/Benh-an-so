package com.benhsoan.domain.medicalrecord.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class MedicalRecordNotOverdueException extends MedicalRecordException {

    public MedicalRecordNotOverdueException(UUID medicalRecordId) {
        super(DomainErrorCode.MEDICAL_RECORD_NOT_OVERDUE,
                "Medical record is not overdue for signing: " + medicalRecordId);
    }

    public MedicalRecordNotOverdueException(String message) {
        super(DomainErrorCode.MEDICAL_RECORD_NOT_OVERDUE, message);
    }
}
