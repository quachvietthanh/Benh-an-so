package com.benhsoan.domain.patient.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PatientAllergyAlreadyExistsException extends PatientException {

    public PatientAllergyAlreadyExistsException(String allergenName) {
        super(DomainErrorCode.PATIENT_ALLERGY_ALREADY_EXISTS,
                "Hoạt chất hoặc nhóm thuốc '" + allergenName + "' đã được ghi nhận trong danh sách dị ứng của bệnh nhân.");
    }
}
