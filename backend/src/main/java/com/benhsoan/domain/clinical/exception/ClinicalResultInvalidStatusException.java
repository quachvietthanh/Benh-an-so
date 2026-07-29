package com.benhsoan.domain.clinical.exception;

import org.springframework.http.HttpStatus;

public class ClinicalResultInvalidStatusException extends ClinicalResultException {

    public ClinicalResultInvalidStatusException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
