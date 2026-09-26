package com.benhsoan.domain.anonymization;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain value describing the current anonymization mode (NCL-15-CN-003).
 * Immutable and framework-free; the enabled flag reflects the persistent
 * system configuration.
 */
public record AnonymizationMode(boolean enabled, Instant updatedAt) {

    public static AnonymizationMode of(boolean enabled, Instant updatedAt) {
        return new AnonymizationMode(enabled, Objects.requireNonNull(updatedAt, "Updated at"));
    }
}
