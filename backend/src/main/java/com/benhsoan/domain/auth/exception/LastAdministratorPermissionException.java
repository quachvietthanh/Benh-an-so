package com.benhsoan.domain.auth.exception;

import org.springframework.http.HttpStatus;

import com.benhsoan.domain.shared.exception.DomainException;

public class LastAdministratorPermissionException extends DomainException {
    public LastAdministratorPermissionException() {
        super(HttpStatus.CONFLICT,
                "Không thể làm tài khoản quản trị viên duy nhất mất quyền quản trị vai trò.");
    }
}
