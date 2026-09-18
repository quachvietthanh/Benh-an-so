package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

public class TemporaryPasswordExpiredException extends AuthException {

    public TemporaryPasswordExpiredException() {
        this("Mật khẩu tạm thời đã hết hạn. Vui lòng liên hệ Quản trị viên để được cấp lại.");
    }

    public TemporaryPasswordExpiredException(String message) {
        super(DomainErrorCode.TEMP_PASSWORD_EXPIRED, message);
    }
}
