package com.benhsoan.domain.druginteraction.exception;

import java.util.UUID;


public class DrugInteractionAlreadyExistsException
        extends DrugInteractionException {

    public DrugInteractionAlreadyExistsException(
            UUID firstMedicineId,
            UUID secondMedicineId
    ) {
        super(
                "Drug interaction already exists between medicines "
                + firstMedicineId
                + " and "
                + secondMedicineId
                + "."
        );
    }
}
