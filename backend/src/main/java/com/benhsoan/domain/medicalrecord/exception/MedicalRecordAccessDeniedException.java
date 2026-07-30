package com.benhsoan.domain.medicalrecord.exception;

import org.springframework.http.HttpStatus;

public class MedicalRecordAccessDeniedException extends MedicalRecordException {

    public MedicalRecordAccessDeniedException() {
        super(HttpStatus.FORBIDDEN, "You do not have permission to view medical history.");
    }
}
