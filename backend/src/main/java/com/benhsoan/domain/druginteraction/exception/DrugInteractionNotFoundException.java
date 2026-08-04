package com.benhsoan.domain.druginteraction.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

public class DrugInteractionNotFoundException
        extends DrugInteractionException {

    public DrugInteractionNotFoundException(UUID drugInteractionId) {
        super(
                HttpStatus.NOT_FOUND,
                "Drug interaction not found with id: "
                + drugInteractionId
        );
    }
}
