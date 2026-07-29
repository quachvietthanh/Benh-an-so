package com.benhsoan.domain.clinical.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public abstract class ClinicalResultException extends DomainException {

    protected ClinicalResultException(HttpStatus status, String message) {
        super(status, message);
    }
}
