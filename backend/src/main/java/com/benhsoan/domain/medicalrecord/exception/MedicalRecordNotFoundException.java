package com.benhsoan.domain.medicalrecord.exception;

import java.util.UUID;


public class MedicalRecordNotFoundException extends MedicalRecordException {

    public MedicalRecordNotFoundException(UUID medicalRecordId) {
        super("Medical record not found: " + medicalRecordId);
    }
}
