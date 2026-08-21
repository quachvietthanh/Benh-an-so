package com.benhsoan.domain.prescription.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class PrescriptionItemNotFoundException
        extends PrescriptionException {

    public PrescriptionItemNotFoundException(UUID prescriptionItemId) {
        super(DomainErrorCode.PRESCRIPTION_ITEM_NOT_FOUND,
                "Prescription item not found with id: "
                + prescriptionItemId
        );
    }
}
