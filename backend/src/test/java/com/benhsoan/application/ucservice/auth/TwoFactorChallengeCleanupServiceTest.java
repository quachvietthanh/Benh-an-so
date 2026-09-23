package com.benhsoan.application.ucservice.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.benhsoan.config.TwoFactorChallengeCleanupProperties;
import com.benhsoan.port.outbound.repository.auth.TwoFactorChallengeRepository;
import com.benhsoan.port.outbound.time.ClockPort;

class TwoFactorChallengeCleanupServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");

    private final TwoFactorChallengeRepository repository = mock(TwoFactorChallengeRepository.class);
    private final ClockPort clockPort = mock(ClockPort.class);

    @Test
    void cleanupDeletesExpiredOrConsumedBeforeThreshold() {
        TwoFactorChallengeCleanupService service =
                new TwoFactorChallengeCleanupService(repository, new TwoFactorChallengeCleanupProperties(30), clockPort);
        when(clockPort.now()).thenReturn(NOW);
        when(repository.deleteExpiredOrConsumedBefore(NOW.minus(Duration.ofDays(30)), NOW)).thenReturn(7);

        int deleted = service.cleanupExpiredChallenges();

        assertEquals(7, deleted);
        verify(repository).deleteExpiredOrConsumedBefore(eq(NOW.minus(Duration.ofDays(30))), eq(NOW));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void cleanupSkipsWhenRetentionDaysIsNotPositive() {
        TwoFactorChallengeCleanupService service =
                new TwoFactorChallengeCleanupService(repository, new TwoFactorChallengeCleanupProperties(0), clockPort);

        int deleted = service.cleanupExpiredChallenges();

        assertEquals(0, deleted);
        verify(repository, never()).deleteExpiredOrConsumedBefore(any(), any());
    }
}