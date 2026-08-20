package com.benhsoan.domain.auth.exception;


public class LastAdministratorPermissionException extends AuthException {
    public LastAdministratorPermissionException() {
        super(
                "Không thể làm tài khoản quản trị viên duy nhất mất quyền quản trị vai trò.");
    }
}
