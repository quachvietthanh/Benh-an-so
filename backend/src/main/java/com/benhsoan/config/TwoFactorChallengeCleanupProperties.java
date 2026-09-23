package com.benhsoan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the periodic cleanup of temporary two-factor challenge
 * records (NCL-01-CN-006). The cleanup removes only challenges that are no
 * longer usable (consumed or expired) and older than the retention window, so
 * it can never delete an active challenge.
 */
@ConfigurationProperties(prefix = "app.security.two-factor.challenge.cleanup")
public record TwoFactorChallengeCleanupProperties(
        int retentionDays
) {
}