package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class SamePasswordException extends AuthException {

    public SamePasswordException() {
        super(DomainErrorCode.SAME_PASSWORD_NOT_ALLOWED, "New password must be different from the old password.");
    }
}
