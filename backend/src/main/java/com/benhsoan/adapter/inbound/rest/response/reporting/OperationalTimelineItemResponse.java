package com.benhsoan.adapter.inbound.rest.response.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OperationalTimelineItemResponse(
        LocalDate date,
        long visitCount,
        BigDecimal revenue
) {
}
