package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class InvalidCredentialsException extends AuthException {

    public InvalidCredentialsException() {
        super(DomainErrorCode.INVALID_CREDENTIALS, "Invalid username or password.");
    }
}
