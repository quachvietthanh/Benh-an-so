package com.benhsoan.adapter.inbound.rest.response.reporting;

import java.time.LocalDate;
import java.util.List;

public record OperationalTimelineResponse(
        LocalDate from,
        LocalDate to,
        List<OperationalTimelineItemResponse> items
) {
}
