package com.benhsoan.domain.auth.exception;

import java.time.Instant;

import com.benhsoan.domain.shared.exception.DomainErrorCode;


public class TooManyLoginAttemptsException extends AuthException {

    private final long retryAfterSeconds;

    private final Instant blockedUntil;

    public TooManyLoginAttemptsException() {
        this(0, null);
    }

    public TooManyLoginAttemptsException(long retryAfterSeconds, Instant blockedUntil) {
        super(DomainErrorCode.TOO_MANY_LOGIN_ATTEMPTS, buildMessage(retryAfterSeconds));
        this.retryAfterSeconds = retryAfterSeconds;
        this.blockedUntil = blockedUntil;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public Instant getBlockedUntil() {
        return blockedUntil;
    }

    private static String buildMessage(long retryAfterSeconds) {
        return retryAfterSeconds > 0
                ? "Hệ thống tạm khóa đăng nhập. Vui lòng thử lại sau " + retryAfterSeconds + " giây."
                : "Hệ thống tạm khóa đăng nhập. Vui lòng thử lại sau.";
    }
}

