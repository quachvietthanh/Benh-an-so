package com.benhsoan.domain.medicalrecord.exception;

import java.util.UUID;


public class MedicalRecordAlreadyExistsForVisitException extends MedicalRecordException {

    public MedicalRecordAlreadyExistsForVisitException(UUID visitId) {
        super("Medical record already exists for visit: " + visitId);
    }
}
