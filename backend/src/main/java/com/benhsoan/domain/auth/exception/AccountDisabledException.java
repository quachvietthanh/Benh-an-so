package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class AccountDisabledException extends AuthException {

    public AccountDisabledException() {
        super(DomainErrorCode.ACCOUNT_DISABLED,
                "Account has been disabled."
        );
    }
}
