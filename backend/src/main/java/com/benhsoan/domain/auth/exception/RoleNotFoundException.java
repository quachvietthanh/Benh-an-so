package com.benhsoan.domain.auth.exception;


public class RoleNotFoundException extends AuthException {

    public RoleNotFoundException() {
        super(
                "Role not found."
        );
    }
}
