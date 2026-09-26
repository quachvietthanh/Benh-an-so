package com.benhsoan.domain.druginteraction.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class DrugInteractionAlreadyExistsException
        extends DrugInteractionException {

    public DrugInteractionAlreadyExistsException(
            UUID firstMedicineId,
            UUID secondMedicineId
    ) {
        super(DomainErrorCode.DRUG_INTERACTION_ALREADY_EXISTS,
                "Drug interaction already exists between medicines "
                + firstMedicineId
                + " and "
                + secondMedicineId
                + "."
        );
    }
}
