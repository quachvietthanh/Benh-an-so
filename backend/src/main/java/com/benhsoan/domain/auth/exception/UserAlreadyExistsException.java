package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class UserAlreadyExistsException extends AuthException {

    public UserAlreadyExistsException() {
        super(DomainErrorCode.USER_ALREADY_EXISTS,
                "Username already exists."
        );
    }
}
