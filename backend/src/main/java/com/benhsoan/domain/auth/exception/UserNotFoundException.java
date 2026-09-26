package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class UserNotFoundException extends AuthException {

    public UserNotFoundException() {
        super(DomainErrorCode.USER_NOT_FOUND,
                "User not found."
        );
    }

    public UserNotFoundException(String username) {
        super(DomainErrorCode.USER_NOT_FOUND,
                "User '" + username + "' not found."
        );
    }
}
