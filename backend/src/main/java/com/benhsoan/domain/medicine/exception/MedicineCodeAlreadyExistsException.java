package com.benhsoan.domain.medicine.exception;


public class MedicineCodeAlreadyExistsException extends MedicineException {

    public MedicineCodeAlreadyExistsException(String medicineCode) {
        super(
                "Medicine code already exists: " + medicineCode
        );
    }
}
