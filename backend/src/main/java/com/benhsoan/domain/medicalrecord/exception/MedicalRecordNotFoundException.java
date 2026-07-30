package com.benhsoan.domain.medicalrecord.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class MedicalRecordNotFoundException extends MedicalRecordException {

    public MedicalRecordNotFoundException(UUID medicalRecordId) {
        super(HttpStatus.NOT_FOUND, "Medical record not found: " + medicalRecordId);
    }
}
