package com.benhsoan.domain.patient.exception;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PatientChronicDiseaseNotFoundException extends PatientException {

    public PatientChronicDiseaseNotFoundException(UUID chronicDiseaseId) {
        super(DomainErrorCode.PATIENT_CHRONIC_DISEASE_NOT_FOUND,
                "Không tìm thấy tiền sử bệnh mạn tính: " + chronicDiseaseId);
    }
}
