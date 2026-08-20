package com.benhsoan.domain.druginteraction.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class SelfDrugInteractionException
        extends DrugInteractionException {

    public SelfDrugInteractionException() {
        super(DomainErrorCode.SELF_DRUG_INTERACTION,
                "A medicine cannot interact with itself."
        );
    }
}
