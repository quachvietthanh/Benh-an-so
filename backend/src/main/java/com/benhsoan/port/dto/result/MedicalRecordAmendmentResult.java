package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

public record MedicalRecordAmendmentResult(
        UUID id, UUID medicalRecordId, String content, String reason, UUID amendedBy, Instant amendedAt
) {
}
