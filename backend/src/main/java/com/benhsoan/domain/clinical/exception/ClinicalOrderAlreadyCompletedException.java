package com.benhsoan.domain.clinical.exception;


public class ClinicalOrderAlreadyCompletedException extends ClinicalOrderException {

    public ClinicalOrderAlreadyCompletedException() {
        super("Clinical order has already been completed.");
    }
}
