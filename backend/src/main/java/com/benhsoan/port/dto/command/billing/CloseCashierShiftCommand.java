package com.benhsoan.port.dto.command.billing;

import java.math.BigDecimal;

public record CloseCashierShiftCommand(
        BigDecimal actualCashAmount,
        String notes
) {
}
