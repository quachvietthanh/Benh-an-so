package com.benhsoan.infrastructure.authSecurity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.persistence.entity.auth.LoginAttemptEntity;
import com.benhsoan.persistence.jpaRepository.auth.JpaLoginAttemptRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class LoginAttemptAdapterTest {

    private static final Instant NOW = Instant.parse("2026-08-26T02:00:00Z");

    @Mock
    private JpaLoginAttemptRepository repository;

    @Mock
    private ClockPort clockPort;

    @Test
    void blocksAfterMaxAttemptsAndReportsAtomicExpiry() {
        LoginAttemptAdapter adapter = new LoginAttemptAdapter(3, 60_000, repository, clockPort);

        Map<String, LoginAttemptEntity> store = new HashMap<>();
        when(repository.findById(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0))));
        when(repository.saveAndFlush(any(LoginAttemptEntity.class))).thenAnswer(inv -> {
            LoginAttemptEntity entity = inv.getArgument(0);
            store.put(entity.getIdentifier(), entity);
            return entity;
        });
        when(repository.atomicIncrement(anyString(), any(Instant.class), org.mockito.ArgumentMatchers.anyInt(), any(Instant.class)))
                .thenAnswer(inv -> {
                    String id = inv.getArgument(0);
                    Instant now = inv.getArgument(1);
                    int max = inv.getArgument(2);
                    Instant blockedUntil = inv.getArgument(3);
                    LoginAttemptEntity entity = store.get(id);
                    if (entity == null) {
                        return 0;
                    }
                    if (entity.getBlockedUntil() != null && !now.isBefore(entity.getBlockedUntil())) {
                        entity.setAttempts(1);
                        entity.setBlockedUntil(null);
                    } else {
                        entity.setAttempts(entity.getAttempts() + 1);
                    }
                    if (entity.getAttempts() >= max && entity.getBlockedUntil() == null) {
                        entity.setBlockedUntil(blockedUntil);
                    }
                    entity.setUpdatedAt(now);
                    return 1;
                });
        when(clockPort.now()).thenReturn(NOW);

        var r1 = adapter.recordLoginFailed("phone");
        assertEquals(1, r1.attemptCount());
        assertFalse(r1.blocked());
        assertFalse(r1.newlyBlocked());

        var r2 = adapter.recordLoginFailed("phone");
        assertEquals(2, r2.attemptCount());
        assertFalse(r2.blocked());
        assertFalse(r2.newlyBlocked());

        var r3 = adapter.recordLoginFailed("phone");
        assertEquals(3, r3.attemptCount());
        assertTrue(r3.blocked());
        assertTrue(r3.newlyBlocked());

        LoginAttemptEntity stored = store.get("phone");
        assertEquals(3, stored.getAttempts());
        assertEquals(NOW.plusMillis(60_000), stored.getBlockedUntil());

        // 4th attempt while already blocked -> blocked=true, newlyBlocked=false
        var r4 = adapter.recordLoginFailed("phone");
        assertEquals(4, r4.attemptCount());
        assertTrue(r4.blocked());
        assertFalse(r4.newlyBlocked());

        // 30 seconds later the block is still active with 30 seconds remaining.
        when(clockPort.now()).thenReturn(NOW.plusMillis(30_000));

        assertTrue(adapter.isBlocked("phone"));
        assertEquals(30L, adapter.getRetryAfterSeconds("phone"));
        assertEquals(NOW.plusMillis(60_000), adapter.getBlockedUntil("phone"));
    }

    @Test
    void expiredBlockIsReleasedAndSuccessClearsRecord() {
        LoginAttemptAdapter adapter = new LoginAttemptAdapter(3, 60_000, repository, clockPort);

        LoginAttemptEntity blocked = new LoginAttemptEntity();
        blocked.setIdentifier("phone");
        blocked.setAttempts(3);
        blocked.setBlockedUntil(NOW.plusMillis(60_000));
        blocked.setUpdatedAt(NOW);

        when(repository.findById("phone")).thenReturn(Optional.of(blocked));
        when(clockPort.now()).thenReturn(NOW.plusMillis(60_001));

        assertFalse(adapter.isBlocked("phone"));
        assertEquals(0L, adapter.getRetryAfterSeconds("phone"));

        adapter.loginSucceeded("phone");
        verify(repository).deleteById("phone");
    }

    @Test
    void unlockDeletesRecord() {
        LoginAttemptAdapter adapter = new LoginAttemptAdapter(5, 900_000, repository, clockPort);
        adapter.unlock("admin_user");
        verify(repository).deleteById("admin_user");
    }
}
