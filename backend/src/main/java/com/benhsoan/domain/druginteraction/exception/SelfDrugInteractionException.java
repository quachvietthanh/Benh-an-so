package com.benhsoan.domain.druginteraction.exception;


public class SelfDrugInteractionException
        extends DrugInteractionException {

    public SelfDrugInteractionException() {
        super(
                "A medicine cannot interact with itself."
        );
    }
}
