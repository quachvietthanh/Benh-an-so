package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class AuthException extends DomainException {

    protected AuthException(DomainErrorCode code, String message) {
        super(code, message);
    }
}
