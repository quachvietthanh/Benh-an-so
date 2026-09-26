package com.benhsoan.port.dto.command.billing;

import java.math.BigDecimal;
import java.util.UUID;

public record GetPaymentQuoteCommand(
        UUID visitId,
        BigDecimal examFee,
        BigDecimal medicineFee
) {
}
