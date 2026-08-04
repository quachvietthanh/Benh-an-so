package com.benhsoan.domain.druginteraction.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class DrugInteractionAlreadyExistsException
        extends DrugInteractionException {

    public DrugInteractionAlreadyExistsException(
            UUID firstMedicineId,
            UUID secondMedicineId
    ) {
        super(
                HttpStatus.CONFLICT,
                "Drug interaction already exists between medicines "
                + firstMedicineId
                + " and "
                + secondMedicineId
                + "."
        );
    }
}
