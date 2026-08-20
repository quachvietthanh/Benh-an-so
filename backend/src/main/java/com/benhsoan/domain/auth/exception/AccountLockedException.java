package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class AccountLockedException extends AuthException {

    public AccountLockedException() {
        super(DomainErrorCode.ACCOUNT_LOCKED,
                "Account has been locked."
        );
    }
}
