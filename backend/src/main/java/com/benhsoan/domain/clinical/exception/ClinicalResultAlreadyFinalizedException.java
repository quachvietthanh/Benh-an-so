package com.benhsoan.domain.clinical.exception;

import org.springframework.http.HttpStatus;

public class ClinicalResultAlreadyFinalizedException extends ClinicalResultException {

    public ClinicalResultAlreadyFinalizedException() {
        super(HttpStatus.CONFLICT, "Clinical result is already finalized.");
    }
}
