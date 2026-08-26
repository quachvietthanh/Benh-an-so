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
        when(repository.save(any(LoginAttemptEntity.class))).thenAnswer(inv -> {
            LoginAttemptEntity entity = inv.getArgument(0);
            store.put(entity.getIdentifier(), entity);
            return entity;
        });
        when(clockPort.now()).thenReturn(NOW);

        adapter.loginFailed("phone");
        adapter.loginFailed("phone");
        adapter.loginFailed("phone");

        LoginAttemptEntity stored = store.get("phone");
        assertEquals(3, stored.getAttempts());
        assertEquals(NOW.plusMillis(60_000), stored.getBlockedUntil());

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
}
