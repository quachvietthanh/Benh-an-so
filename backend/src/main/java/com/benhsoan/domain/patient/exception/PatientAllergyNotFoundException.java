package com.benhsoan.domain.patient.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PatientAllergyNotFoundException extends PatientException {

    public PatientAllergyNotFoundException(UUID allergyId) {
        super(DomainErrorCode.PATIENT_ALLERGY_NOT_FOUND, "Không tìm thấy thông tin dị ứng: " + allergyId);
    }
}
