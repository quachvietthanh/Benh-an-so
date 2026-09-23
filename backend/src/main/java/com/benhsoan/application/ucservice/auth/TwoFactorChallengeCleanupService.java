package com.benhsoan.application.ucservice.auth;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.config.TwoFactorChallengeCleanupProperties;
import com.benhsoan.port.inbound.auth.TwoFactorChallengeCleanupUseCase;
import com.benhsoan.port.outbound.repository.auth.TwoFactorChallengeRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwoFactorChallengeCleanupService implements TwoFactorChallengeCleanupUseCase {

    private final TwoFactorChallengeRepository challengeRepository;
    private final TwoFactorChallengeCleanupProperties properties;
    private final ClockPort clockPort;

    @Override
    @Transactional
    public int cleanupExpiredChallenges() {
        int retentionDays = properties.retentionDays();
        if (retentionDays <= 0) {
            log.warn("Two-factor challenge cleanup skipped: retention-days must be positive (was {}).", retentionDays);
            return 0;
        }

        Instant now = clockPort.now();
        Instant retentionThreshold = now.minus(Duration.ofDays(retentionDays));

        int deleted = challengeRepository.deleteExpiredOrConsumedBefore(retentionThreshold, now);
        log.info("Cleaned up {} two-factor challenge record(s) older than {} day(s).", deleted, retentionDays);
        return deleted;
    }
}