package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class RoleNotFoundException extends AuthException {

    public RoleNotFoundException() {
        super(DomainErrorCode.ROLE_NOT_FOUND,
                "Role not found."
        );
    }
}
