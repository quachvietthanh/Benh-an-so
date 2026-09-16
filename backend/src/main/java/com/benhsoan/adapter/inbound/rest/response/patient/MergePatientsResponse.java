package com.benhsoan.adapter.inbound.rest.response.patient;

import java.time.Instant;
import java.util.UUID;

public record MergePatientsResponse(

        UUID sourcePatientId,

        String sourcePatientCode,

        UUID targetPatientId,

        String targetPatientCode,

        int transferredVisitsCount,

        UUID mergedBy,

        String reason,

        Instant mergedAt

) {}
