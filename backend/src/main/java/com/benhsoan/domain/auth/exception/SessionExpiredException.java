package com.benhsoan.domain.auth.exception;


public class SessionExpiredException extends AuthException {

    public SessionExpiredException() {
        super(
                "Session has expired."
        );
    }
}
