package com.benhsoan.domain.druginteraction.exception;

import org.springframework.http.HttpStatus;

public class SelfDrugInteractionException
        extends DrugInteractionException {

    public SelfDrugInteractionException() {
        super(
                HttpStatus.BAD_REQUEST,
                "A medicine cannot interact with itself."
        );
    }
}
