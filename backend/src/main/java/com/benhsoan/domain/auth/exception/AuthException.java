package com.benhsoan.domain.auth.exception;


import com.benhsoan.domain.shared.exception.DomainException;

public abstract class AuthException extends DomainException {

    protected AuthException(String message) {
        super(message);
    }
}
