package com.benhsoan.port.outbound.authSecurity;

import java.time.Instant;

public interface LoginAttemptPort {

    void loginSucceeded(String identifier);

    void loginFailed(String identifier);

    boolean isBlocked(String identifier);

    long getRetryAfterSeconds(String identifier);

    Instant getBlockedUntil(String identifier);

    int getAttemptCount(String identifier);
}