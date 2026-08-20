package com.benhsoan.domain.clinical.exception;


public class ClinicalOrderInvalidVisitException extends ClinicalOrderException {

    public ClinicalOrderInvalidVisitException() {
        super("Clinical orders can only be created for an active visit.");
    }
}
