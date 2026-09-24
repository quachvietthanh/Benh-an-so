package com.benhsoan.domain.session;

import java.time.Duration;

/**
 * Technical session settings derived from the clinic configuration.
 * The inactivity timeout and warning threshold are business-configurable;
 * the absolute session lifetime (refresh token TTL) is managed separately.
 */
public record SessionSettings(
        Duration inactivityTimeout,
        Duration warningThreshold
) {
}