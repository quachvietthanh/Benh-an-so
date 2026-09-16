package com.benhsoan.port.dto.result.patient;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;

@Builder
public record MergePatientsResult(
        UUID sourcePatientId,
        String sourcePatientCode,
        UUID targetPatientId,
        String targetPatientCode,
        int transferredVisitsCount,
        UUID mergedBy,
        String reason,
        Instant mergedAt
) {
}
