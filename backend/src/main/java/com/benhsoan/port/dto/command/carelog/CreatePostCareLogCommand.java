package com.benhsoan.port.dto.command.carelog;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.carelog.enums.ContactChannel;
import com.benhsoan.domain.carelog.enums.ContactOutcome;
import com.benhsoan.domain.carelog.enums.PatientCondition;

public record CreatePostCareLogCommand(
        UUID patientId,
        UUID reminderId,
        UUID visitId,
        ContactChannel contactChannel,
        Instant contactedAt,
        PatientCondition patientCondition,
        String careNotes,
        ContactOutcome contactOutcome
) {
}
