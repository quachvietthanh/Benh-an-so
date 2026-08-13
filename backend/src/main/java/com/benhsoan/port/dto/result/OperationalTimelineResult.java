package com.benhsoan.port.dto.result;

import java.time.LocalDate;
import java.util.List;

public record OperationalTimelineResult(
        LocalDate from,
        LocalDate to,
        List<OperationalTimelineItemResult> items
) {
}
