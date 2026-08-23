package com.benhsoan.domain.medicalrecord.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class MedicalRecordMissingDiagnosisException extends MedicalRecordException {

    public MedicalRecordMissingDiagnosisException(UUID medicalRecordId) {
        super(DomainErrorCode.MEDICAL_RECORD_MISSING_DIAGNOSIS,
                "Medical record requires at least one diagnosis before signing: " + medicalRecordId);
    }
}
