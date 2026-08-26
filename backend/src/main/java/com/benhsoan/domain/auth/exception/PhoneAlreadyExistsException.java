package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class PhoneAlreadyExistsException extends AuthException {

    public PhoneAlreadyExistsException() {
        this("Phone number already exists.");
    }

    public PhoneAlreadyExistsException(String message) {
        super(DomainErrorCode.PHONE_ALREADY_EXISTS, message);
    }
}
