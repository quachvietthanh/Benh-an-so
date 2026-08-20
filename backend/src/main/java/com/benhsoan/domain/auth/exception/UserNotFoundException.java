package com.benhsoan.domain.auth.exception;


public class UserNotFoundException extends AuthException {

    public UserNotFoundException() {
        super(
                "User not found."
        );
    }

    public UserNotFoundException(String username) {
        super(
                "User '" + username + "' not found."
        );
    }
}
