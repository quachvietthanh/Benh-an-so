package com.benhsoan.adapter.inbound.rest.response.carelog;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.carelog.enums.ContactChannel;
import com.benhsoan.domain.carelog.enums.ContactOutcome;
import com.benhsoan.domain.carelog.enums.PatientCondition;

public record PostCareLogResponse(
        UUID id,
        UUID patientId,
        UUID reminderId,
        UUID visitId,
        ContactChannel contactChannel,
        Instant contactedAt,
        PatientCondition patientCondition,
        String careNotes,
        ContactOutcome contactOutcome,
        UUID performedBy,
        Instant createdAt
) {
}
