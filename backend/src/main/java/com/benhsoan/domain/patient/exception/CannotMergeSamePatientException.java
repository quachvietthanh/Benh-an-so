package com.benhsoan.domain.patient.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class CannotMergeSamePatientException extends PatientException {

    public CannotMergeSamePatientException() {
        super(DomainErrorCode.CANNOT_MERGE_SAME_PATIENT,
                "Không thể gộp một hồ sơ bệnh nhân vào chính nó.");
    }
}
