package com.benhsoan.domain.security.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;

import com.benhsoan.domain.shared.exception.DomainException;

public class SecurityAlertNotFoundException extends DomainException {

    public SecurityAlertNotFoundException(UUID id) {
        super(DomainErrorCode.SECURITY_ALERT_NOT_FOUND, "Security alert not found: " + id);
    }
}
