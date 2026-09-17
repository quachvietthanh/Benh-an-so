package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

/**
 * Thrown when a patient verification code is invalid or attempts exceeded.
 */
public class InvalidVerificationCodeException extends AuthException {

    public InvalidVerificationCodeException() {
        super(DomainErrorCode.INVALID_VERIFICATION_CODE, "Mã xác thực không chính xác hoặc không tồn tại.");
    }

    public InvalidVerificationCodeException(String message) {
        super(DomainErrorCode.INVALID_VERIFICATION_CODE, message);
    }
}
