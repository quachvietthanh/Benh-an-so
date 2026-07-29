package com.benhsoan.domain.clinical.exception;

import org.springframework.http.HttpStatus;

public class ClinicalOrderInvalidStatusException extends ClinicalOrderException {

    public ClinicalOrderInvalidStatusException(String m) {
        super(HttpStatus.CONFLICT, m);
    }
}
