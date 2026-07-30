package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.time.Instant;
import java.util.UUID;

public record MedicalRecordAmendmentResponse(
        UUID id, UUID medicalRecordId, String content, String reason, UUID amendedBy, Instant amendedAt
) {
}
