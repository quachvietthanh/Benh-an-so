package com.benhsoan.port.dto.command.appointment;

import java.time.LocalDate;
import java.util.UUID;

import com.benhsoan.domain.appointment.enums.TimePreference;

import lombok.Builder;

@Builder
public record AddToWaitlistCommand(
        UUID patientId,
        UUID doctorId,
        LocalDate desiredDate,
        TimePreference timePreference,
        String note
) {
}
