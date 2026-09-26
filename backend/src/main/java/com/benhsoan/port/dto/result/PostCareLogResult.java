package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.carelog.enums.ContactChannel;
import com.benhsoan.domain.carelog.enums.ContactOutcome;
import com.benhsoan.domain.carelog.enums.PatientCondition;

public record PostCareLogResult(
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
