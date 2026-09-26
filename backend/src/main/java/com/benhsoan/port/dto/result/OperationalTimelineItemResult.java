package com.benhsoan.port.dto.result;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OperationalTimelineItemResult(
        LocalDate date,
        long visitCount,
        BigDecimal revenue
) {
}
