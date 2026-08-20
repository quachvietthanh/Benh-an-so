package com.benhsoan.domain.medicine.exception;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class MedicineException extends DomainException {

    protected MedicineException(
            String message
    ) {
        super(message);
    }
}
