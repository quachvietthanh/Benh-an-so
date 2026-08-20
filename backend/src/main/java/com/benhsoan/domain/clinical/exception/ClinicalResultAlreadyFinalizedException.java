package com.benhsoan.domain.clinical.exception;


public class ClinicalResultAlreadyFinalizedException extends ClinicalResultException {

    public ClinicalResultAlreadyFinalizedException() {
        super("Clinical result is already finalized.");
    }
}
