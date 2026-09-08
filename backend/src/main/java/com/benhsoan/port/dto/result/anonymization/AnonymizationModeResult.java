package com.benhsoan.port.dto.result.anonymization;

import java.time.Instant;

public record AnonymizationModeResult(boolean enabled, Instant updatedAt) {
}
