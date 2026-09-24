package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class SessionNotFoundException extends AuthException {

    public SessionNotFoundException() {
        super(DomainErrorCode.SESSION_NOT_FOUND,
                "Session not found."
        );
    }
}