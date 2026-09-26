package com.benhsoan.domain.druginteraction.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;


public class DrugInteractionNotFoundException
        extends DrugInteractionException {

    public DrugInteractionNotFoundException(UUID drugInteractionId) {
        super(DomainErrorCode.DRUG_INTERACTION_NOT_FOUND,
                "Drug interaction not found with id: "
                + drugInteractionId
        );
    }
}
