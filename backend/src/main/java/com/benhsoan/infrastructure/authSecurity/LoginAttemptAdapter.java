package com.benhsoan.infrastructure.authSecurity;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.persistence.entity.auth.LoginAttemptEntity;
import com.benhsoan.persistence.jpaRepository.auth.JpaLoginAttemptRepository;
import com.benhsoan.port.outbound.authSecurity.LoginAttemptPort;
import com.benhsoan.port.outbound.time.ClockPort;

/**
 * Persistent login-attempt tracking with atomic {@code blocked_until} expiry (NCL-14-CN-002 TC-02).
 * Writes run in a REQUIRES_NEW transaction so failed-attempt increments survive the rollback of the
 * login use-case when credentials are rejected.
 */
@Component
public class LoginAttemptAdapter implements LoginAttemptPort {

    private final int maxAttempts;

    private final long blockDurationMs;

    private final JpaLoginAttemptRepository repository;

    private final ClockPort clockPort;

    public LoginAttemptAdapter(
            @Value("${app.security.login.max-attempts:5}") int maxAttempts,
            @Value("${app.security.login.block-duration-ms:900000}") long blockDurationMs,
            JpaLoginAttemptRepository repository,
            ClockPort clockPort
    ) {
        this.maxAttempts = maxAttempts;
        this.blockDurationMs = blockDurationMs;
        this.repository = repository;
        this.clockPort = clockPort;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loginSucceeded(String identifier) {
        repository.deleteById(identifier);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loginFailed(String identifier) {
        Instant now = clockPort.now();
        LoginAttemptEntity entity = repository.findById(identifier).orElse(null);

        if (entity == null) {
            entity = new LoginAttemptEntity();
            entity.setIdentifier(identifier);
            entity.setAttempts(0);
            entity.setBlockedUntil(null);
        } else if (isExpired(entity, now)) {
            entity.setAttempts(0);
            entity.setBlockedUntil(null);
        }

        int attempts = entity.getAttempts() + 1;
        entity.setAttempts(attempts);
        entity.setUpdatedAt(now);
        if (attempts >= maxAttempts) {
            entity.setBlockedUntil(now.plusMillis(blockDurationMs));
        }
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(String identifier) {
        LoginAttemptEntity entity = repository.findById(identifier).orElse(null);
        if (entity == null || entity.getBlockedUntil() == null) {
            return false;
        }
        return clockPort.now().isBefore(entity.getBlockedUntil());
    }

    @Override
    @Transactional(readOnly = true)
    public long getRetryAfterSeconds(String identifier) {
        LoginAttemptEntity entity = repository.findById(identifier).orElse(null);
        if (entity == null || entity.getBlockedUntil() == null) {
            return 0;
        }
        long millis = Duration.between(clockPort.now(), entity.getBlockedUntil()).toMillis();
        if (millis <= 0) {
            return 0;
        }
        return (long) Math.ceil(millis / 1000.0);
    }

    @Override
    @Transactional(readOnly = true)
    public Instant getBlockedUntil(String identifier) {
        LoginAttemptEntity entity = repository.findById(identifier).orElse(null);
        if (entity == null) {
            return null;
        }
        Instant blockedUntil = entity.getBlockedUntil();
        if (blockedUntil != null && !clockPort.now().isBefore(blockedUntil)) {
            return null;
        }
        return blockedUntil;
    }

    @Override
    @Transactional(readOnly = true)
    public int getAttemptCount(String identifier) {
        LoginAttemptEntity entity = repository.findById(identifier).orElse(null);
        return entity == null ? 0 : entity.getAttempts();
    }

    private boolean isExpired(LoginAttemptEntity entity, Instant now) {
        return entity.getBlockedUntil() != null && !now.isBefore(entity.getBlockedUntil());
    }
}
