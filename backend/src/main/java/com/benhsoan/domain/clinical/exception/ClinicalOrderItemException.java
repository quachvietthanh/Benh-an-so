package com.benhsoan.domain.clinical.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public abstract class ClinicalOrderItemException extends DomainException {

    protected ClinicalOrderItemException(HttpStatus status, String message) {
        super(status, message);
    }
}
