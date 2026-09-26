package com.benhsoan.port.dto.command.billing;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

import com.benhsoan.domain.billing.enums.CashierShiftStatus;

public record SearchCashierShiftsQuery(
        UUID cashierId,
        CashierShiftStatus status,
        Instant from,
        Instant to,
        Pageable pageable
) {
}
