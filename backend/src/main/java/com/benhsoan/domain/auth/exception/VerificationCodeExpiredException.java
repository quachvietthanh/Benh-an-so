package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

/**
 * Thrown when a patient verification code has expired (NCL-14-CN-006 TC-02).
 */
public class VerificationCodeExpiredException extends AuthException {

    public VerificationCodeExpiredException() {
        super(DomainErrorCode.VERIFICATION_CODE_EXPIRED, "Mã xác thực đã hết hạn. Vui lòng yêu cầu mã mới.");
    }
}
