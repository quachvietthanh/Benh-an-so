package com.benhsoan.port.dto.command.billing;

import java.util.UUID;

public record ConfirmCashierShiftCommand(
        UUID shiftId,
        String confirmationNotes
) {
}
