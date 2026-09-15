package com.benhsoan.domain.patient.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PatientChronicDiseaseAlreadyExistsException extends PatientException {

    public PatientChronicDiseaseAlreadyExistsException() {
        super(DomainErrorCode.PATIENT_CHRONIC_DISEASE_ALREADY_EXISTS,
                "Bệnh mạn tính đã được ghi nhận trong tiền sử của bệnh nhân.");
    }
}
