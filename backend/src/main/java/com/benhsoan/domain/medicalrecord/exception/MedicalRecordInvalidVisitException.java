package com.benhsoan.domain.medicalrecord.exception;

import java.util.UUID;


public class MedicalRecordInvalidVisitException extends MedicalRecordException {

    public MedicalRecordInvalidVisitException(UUID visitId) {
        super("Medical record operation is not allowed for inactive visit: " + visitId);
    }
}
