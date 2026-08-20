package com.benhsoan.domain.medicalrecord.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class MedicalRecordAmendmentRequiresCompletedVisitException extends MedicalRecordException {

    public MedicalRecordAmendmentRequiresCompletedVisitException() {
        super(DomainErrorCode.MEDICAL_RECORD_AMENDMENT_REQUIRES_COMPLETED_VISIT, "Medical record can only be amended after the visit is completed.");
    }
}
