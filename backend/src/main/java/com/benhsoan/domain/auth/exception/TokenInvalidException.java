package com.benhsoan.domain.auth.exception;


public class TokenInvalidException extends AuthException {

    public TokenInvalidException() {
        super(
                "Token is invalid."
        );
    }
}
