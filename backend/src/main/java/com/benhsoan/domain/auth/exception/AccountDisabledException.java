package com.benhsoan.domain.auth.exception;


public class AccountDisabledException extends AuthException {

    public AccountDisabledException() {
        super(
                "Account has been disabled."
        );
    }
}
