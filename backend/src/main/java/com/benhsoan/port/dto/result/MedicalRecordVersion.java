package com.benhsoan.port.dto.result;

import java.time.Instant;

/**
 * A single version entry in a medical record's version history.
 * Version 1 is the original record; versions 2..N are amendments.
 */
public record MedicalRecordVersion(
        int versionNumber,
        String modifiedBy,
        Instant modifiedAt,
        String reason,
        String content
) {
}
