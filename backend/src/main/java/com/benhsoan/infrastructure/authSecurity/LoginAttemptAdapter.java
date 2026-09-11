package com.benhsoan.infrastructure.authSecurity;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.benhsoan.persistence.entity.auth.LoginAttemptEntity;
import com.benhsoan.persistence.jpaRepository.auth.JpaLoginAttemptRepository;
import com.benhsoan.port.dto.result.LoginAttemptResult;
import com.benhsoan.port.outbound.authSecurity.LoginAttemptPort;
import com.benhsoan.port.outbound.time.ClockPort;

/**
 * Persistent login-attempt tracking with atomic {@code blocked_until} expiry
 * (NCL-14-CN-002 TC-02).
 * Writes run in a REQUIRES_NEW transaction so failed-attempt increments survive
 * the rollback of the
 * login use-case when credentials are rejected.
 */
@Component
public class LoginAttemptAdapter implements LoginAttemptPort {

    private final int maxAttempts;

    private final long blockDurationMs;

    private final JpaLoginAttemptRepository repository;

    private final ClockPort clockPort;

    private final TransactionTemplate transactionTemplate;

    @Autowired
    public LoginAttemptAdapter(
            @Value("${app.security.login.max-attempts:5}") int maxAttempts,
            @Value("${app.security.login.block-duration-ms:900000}") long blockDurationMs,
            JpaLoginAttemptRepository repository,
            ClockPort clockPort,
            @Autowired(required = false) PlatformTransactionManager transactionManager) {
        this.maxAttempts = maxAttempts;
        this.blockDurationMs = blockDurationMs;
        this.repository = repository;
        this.clockPort = clockPort;
        if (transactionManager != null) {
            TransactionTemplate tt = new TransactionTemplate(transactionManager);
            tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            this.transactionTemplate = tt;
        } else {
            this.transactionTemplate = null;
        }
    }

    public LoginAttemptAdapter(
            int maxAttempts,
            long blockDurationMs,
            JpaLoginAttemptRepository repository,
            ClockPort clockPort) {
        this(maxAttempts, blockDurationMs, repository, clockPort, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loginSucceeded(String identifier) {
        repository.deleteById(identifier);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void unlock(String identifier) {
        repository.deleteById(identifier);
    }

    @Override
    public LoginAttemptResult recordLoginFailed(String identifier) {
        Instant now = clockPort.now();
        Instant newBlockedUntil = now.plusMillis(blockDurationMs);

        for (int i = 0; i < 20; i++) {
            if (i > 0) {
                Thread.yield();
            }
            LoginAttemptResult result = executeInTransaction(status -> {
                int updatedRows = repository.atomicIncrement(identifier, now, maxAttempts, newBlockedUntil);
                if (updatedRows > 0) {
                    LoginAttemptEntity entity = repository.findById(identifier).orElse(null);
                    if (entity == null) {
                        return null;
                    }
                    boolean blocked = entity.getBlockedUntil() != null && now.isBefore(entity.getBlockedUntil());
                    boolean newlyBlocked = blocked && (entity.getAttempts() == maxAttempts);
                    long retryAfter = calculateRetryAfterSeconds(entity.getBlockedUntil(), now);
                    return new LoginAttemptResult(
                            entity.getAttempts(),
                            blocked,
                            newlyBlocked,
                            blocked ? entity.getBlockedUntil() : null,
                            retryAfter);
                } else {
                    LoginAttemptEntity newEntity = new LoginAttemptEntity();
                    newEntity.setIdentifier(identifier);
                    newEntity.setAttempts(1);
                    newEntity.setUpdatedAt(now);
                    if (maxAttempts <= 1) {
                        newEntity.setBlockedUntil(newBlockedUntil);
                    } else {
                        newEntity.setBlockedUntil(null);
                    }
                    try {
                        repository.saveAndFlush(newEntity);
                        boolean blocked = newEntity.getBlockedUntil() != null
                                && now.isBefore(newEntity.getBlockedUntil());
                        boolean newlyBlocked = blocked && (newEntity.getAttempts() == maxAttempts);
                        long retryAfter = calculateRetryAfterSeconds(newEntity.getBlockedUntil(), now);
                        return new LoginAttemptResult(
                                newEntity.getAttempts(),
                                blocked,
                                newlyBlocked,
                                blocked ? newEntity.getBlockedUntil() : null,
                                retryAfter);
                    } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                        status.setRollbackOnly();
                        return null;
                    }
                }
            });

            if (result != null) {
                return result;
            }
        }

        LoginAttemptEntity entity = repository.findById(identifier).orElse(null);
        int attempts = entity != null ? entity.getAttempts() : 1;
        boolean blocked = entity != null && entity.getBlockedUntil() != null && now.isBefore(entity.getBlockedUntil());
        boolean newlyBlocked = blocked && (attempts == maxAttempts);
        Instant blockedUntil = (blocked && entity != null) ? entity.getBlockedUntil() : null;
        long retryAfter = calculateRetryAfterSeconds(blockedUntil, now);
        return new LoginAttemptResult(attempts, blocked, newlyBlocked, blockedUntil, retryAfter);
    }

    @Override
    public void loginFailed(String identifier) {
        recordLoginFailed(identifier);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public boolean isBlocked(String identifier) {
        LoginAttemptEntity entity = repository.findById(identifier).orElse(null);
        if (entity == null || entity.getBlockedUntil() == null) {
            return false;
        }
        return clockPort.now().isBefore(entity.getBlockedUntil());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
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
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
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
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public int getAttemptCount(String identifier) {
        LoginAttemptEntity entity = repository.findById(identifier).orElse(null);
        return entity == null ? 0 : entity.getAttempts();
    }

    private long calculateRetryAfterSeconds(Instant blockedUntil, Instant now) {
        if (blockedUntil == null) {
            return 0;
        }
        long millis = Duration.between(now, blockedUntil).toMillis();
        if (millis <= 0) {
            return 0;
        }
        return (long) Math.ceil(millis / 1000.0);
    }

    private <T> T executeInTransaction(TransactionCallback<T> action) {
        if (transactionTemplate != null) {
            return transactionTemplate.execute(action);
        } else {
            return action.doInTransaction(new SimpleTransactionStatus());
        }
    }
}
