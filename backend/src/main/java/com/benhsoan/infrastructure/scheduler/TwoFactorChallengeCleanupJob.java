package com.benhsoan.infrastructure.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.benhsoan.port.inbound.auth.TwoFactorChallengeCleanupUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.security.two-factor.challenge.cleanup.enabled", havingValue = "true")
public class TwoFactorChallengeCleanupJob {

    private final TwoFactorChallengeCleanupUseCase cleanupUseCase;

    @Scheduled(cron = "${app.security.two-factor.challenge.cleanup.cron}")
    public void cleanup() {
        cleanupUseCase.cleanupExpiredChallenges();
    }
}