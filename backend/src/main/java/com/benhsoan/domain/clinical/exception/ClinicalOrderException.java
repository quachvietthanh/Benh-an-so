package com.benhsoan.domain.clinical.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public abstract class ClinicalOrderException extends DomainException {

    protected ClinicalOrderException(HttpStatus s, String m) {
        super(s, m);
    }
}
