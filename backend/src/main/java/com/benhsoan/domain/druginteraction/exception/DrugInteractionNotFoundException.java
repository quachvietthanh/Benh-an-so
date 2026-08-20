package com.benhsoan.domain.druginteraction.exception;

import java.util.UUID;


public class DrugInteractionNotFoundException
        extends DrugInteractionException {

    public DrugInteractionNotFoundException(UUID drugInteractionId) {
        super(
                "Drug interaction not found with id: "
                + drugInteractionId
        );
    }
}
