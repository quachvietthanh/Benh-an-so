package com.benhsoan.domain.auth.exception;


public class UserAlreadyExistsException extends AuthException {

    public UserAlreadyExistsException() {
        super(
                "Username already exists."
        );
    }
}
