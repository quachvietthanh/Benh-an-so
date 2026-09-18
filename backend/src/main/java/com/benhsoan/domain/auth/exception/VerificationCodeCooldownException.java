package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

/**
 * Thrown when requesting a new verification code too quickly.
 */
public class VerificationCodeCooldownException extends AuthException {

    public VerificationCodeCooldownException(long retryAfterSeconds) {
        super(DomainErrorCode.VERIFICATION_CODE_COOLDOWN,
                "Vui lòng đợi " + retryAfterSeconds + " giây trước khi yêu cầu mã xác thực mới.");
    }
}
