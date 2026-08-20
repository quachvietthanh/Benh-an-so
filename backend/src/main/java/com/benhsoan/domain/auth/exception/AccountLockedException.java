package com.benhsoan.domain.auth.exception;


public class AccountLockedException extends AuthException {

    public AccountLockedException() {
        super(
                "Account has been locked."
        );
    }
}
