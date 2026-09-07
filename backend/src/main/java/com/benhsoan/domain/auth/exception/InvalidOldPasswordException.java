package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class InvalidOldPasswordException extends AuthException {

    public InvalidOldPasswordException() {
        super(DomainErrorCode.INVALID_OLD_PASSWORD, "Current password is incorrect.");
    }
}
