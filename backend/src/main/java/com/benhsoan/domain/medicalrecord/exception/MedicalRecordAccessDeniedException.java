package com.benhsoan.domain.medicalrecord.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public class MedicalRecordAccessDeniedException extends DomainException {

    public MedicalRecordAccessDeniedException() {
        super(HttpStatus.FORBIDDEN, "You do not have permission to view medical history.");
    }
}
