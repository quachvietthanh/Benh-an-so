package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class LastAdministratorPermissionException extends AuthException {
    public LastAdministratorPermissionException() {
        super(DomainErrorCode.LAST_ADMINISTRATOR_PERMISSION,
                "Không thể làm tài khoản quản trị viên duy nhất mất quyền quản trị vai trò.");
    }
}
