package com.benhsoan.domain.auth.exception;


public class TooManyLoginAttemptsException extends AuthException {

    public TooManyLoginAttemptsException() {
        super(
                "Too many login attempts."
        );
    }
}
