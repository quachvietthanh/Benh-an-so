package com.benhsoan.domain.medicalrecord.exception;


public class MedicalRecordAccessDeniedException extends MedicalRecordException {

    public MedicalRecordAccessDeniedException() {
        super("You do not have permission to view medical history.");
    }
}
