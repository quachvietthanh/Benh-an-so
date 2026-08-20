package com.benhsoan.domain.medicalrecord.exception;


public class MedicalRecordAmendmentRequiresCompletedVisitException extends MedicalRecordException {

    public MedicalRecordAmendmentRequiresCompletedVisitException() {
        super("Medical record can only be amended after the visit is completed.");
    }
}
