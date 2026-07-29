package com.benhsoan.domain.clinical.exception;

import org.springframework.http.HttpStatus;

public class ClinicalOrderItemInvalidStatusException extends ClinicalOrderItemException {

    public ClinicalOrderItemInvalidStatusException(String m) {
        super(HttpStatus.CONFLICT, m);
    }
}
