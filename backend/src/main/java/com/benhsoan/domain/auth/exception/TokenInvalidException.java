package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class TokenInvalidException extends AuthException {

    public TokenInvalidException() {
        super(DomainErrorCode.TOKEN_INVALID,
                "Token is invalid."
        );
    }
}
