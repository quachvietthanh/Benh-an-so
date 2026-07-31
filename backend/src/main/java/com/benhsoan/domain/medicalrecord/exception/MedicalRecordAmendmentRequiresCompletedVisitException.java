package com.benhsoan.domain.medicalrecord.exception;

import org.springframework.http.HttpStatus;

public class MedicalRecordAmendmentRequiresCompletedVisitException extends MedicalRecordException {

    public MedicalRecordAmendmentRequiresCompletedVisitException() {
        super(HttpStatus.CONFLICT, "Medical record can only be amended after the visit is completed.");
    }
}
