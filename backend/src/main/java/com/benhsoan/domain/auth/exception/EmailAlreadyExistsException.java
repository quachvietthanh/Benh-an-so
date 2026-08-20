package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class EmailAlreadyExistsException extends AuthException {

    public EmailAlreadyExistsException() {
        super(DomainErrorCode.EMAIL_ALREADY_EXISTS,
                "Email already exists."
        );
    }
}
