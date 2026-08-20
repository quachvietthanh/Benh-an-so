package com.benhsoan.domain.clinical.exception;


public class ClinicalOrderAlreadyCancelledException extends ClinicalOrderException {

    public ClinicalOrderAlreadyCancelledException() {
        super("Clinical order has already been cancelled.");
    }
}
