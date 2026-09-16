package com.benhsoan.domain.patient.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PatientFamilyHistoryNotFoundException extends PatientException {

    public PatientFamilyHistoryNotFoundException(UUID familyHistoryId) {
        super(DomainErrorCode.PATIENT_FAMILY_HISTORY_NOT_FOUND,
                "Không tìm thấy tiền sử gia đình: " + familyHistoryId);
    }
}
