package com.benhsoan.port.dto.command.prescription;

import java.util.List;
import java.util.UUID;

public record CheckMaxDailyDoseCommand(
        UUID medicalRecordId,
        List<CheckMaxDailyDoseItemCommand> items
) {
}
