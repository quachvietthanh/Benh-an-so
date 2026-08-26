package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PhoneAlreadyExistsException extends AuthException {

    public PhoneAlreadyExistsException() {
        super(DomainErrorCode.PHONE_ALREADY_EXISTS,
                "Phone number already exists."
        );
    }
}
