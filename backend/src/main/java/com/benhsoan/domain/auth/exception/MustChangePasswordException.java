package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class MustChangePasswordException extends AuthException {

    public MustChangePasswordException() {
        super(DomainErrorCode.MUST_CHANGE_PASSWORD, "Tài khoản đang yêu cầu đổi mật khẩu trước khi tiếp tục thao tác.");
    }
}
