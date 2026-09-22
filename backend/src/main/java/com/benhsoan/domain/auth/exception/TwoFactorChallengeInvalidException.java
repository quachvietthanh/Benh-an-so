package com.benhsoan.domain.auth.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

/**
 * Thrown when a two-factor authentication challenge is invalid, not found,
 * already consumed, or otherwise can no longer be used to authenticate.
 */
public class TwoFactorChallengeInvalidException extends AuthException {

    public TwoFactorChallengeInvalidException() {
        super(DomainErrorCode.TWO_FACTOR_CHALLENGE_INVALID,
                "Thử thách xác thực hai lớp không hợp lệ hoặc đã được sử dụng.");
    }

    public TwoFactorChallengeInvalidException(String message) {
        super(DomainErrorCode.TWO_FACTOR_CHALLENGE_INVALID, message);
    }
}
