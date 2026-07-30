package com.benhsoan.domain.visit.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public class VisitNotFoundException extends DomainException {

    public VisitNotFoundException(UUID visitId) {
        super(HttpStatus.NOT_FOUND, "Visit not found: " + visitId);
    }
}
