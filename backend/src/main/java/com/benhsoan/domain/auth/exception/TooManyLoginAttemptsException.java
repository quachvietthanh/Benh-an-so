package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class TooManyLoginAttemptsException extends AuthException {

    public TooManyLoginAttemptsException() {
        super(DomainErrorCode.TOO_MANY_LOGIN_ATTEMPTS,
                "Too many login attempts."
        );
    }
}
