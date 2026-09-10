package com.benhsoan.port.outbound.authSecurity;

import java.time.Instant;

import com.benhsoan.port.dto.result.LoginAttemptResult;

public interface LoginAttemptPort {

    void loginSucceeded(String identifier);

    LoginAttemptResult recordLoginFailed(String identifier);

    default void loginFailed(String identifier) {
        recordLoginFailed(identifier);
    }

    void unlock(String identifier);

    boolean isBlocked(String identifier);

    long getRetryAfterSeconds(String identifier);

    Instant getBlockedUntil(String identifier);

    int getAttemptCount(String identifier);
}