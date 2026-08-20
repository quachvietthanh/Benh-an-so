package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class SessionExpiredException extends AuthException {

    public SessionExpiredException() {
        super(DomainErrorCode.SESSION_EXPIRED,
                "Session has expired."
        );
    }
}
