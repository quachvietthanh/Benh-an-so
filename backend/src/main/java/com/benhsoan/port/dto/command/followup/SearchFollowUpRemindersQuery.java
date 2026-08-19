package com.benhsoan.port.dto.command.followup;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.followup.enums.ReminderStatus;

public record SearchFollowUpRemindersQuery(
        UUID patientId,
        ReminderStatus status,
        LocalDate fromDate,
        LocalDate toDate,
        Pageable pageable
) {
}
