package com.benhsoan.application.ucservice.reporting;

import com.benhsoan.port.dto.result.OperationalSummaryResult;
import com.benhsoan.port.dto.result.OperationalTimelineResult;

public record OperationalReportData(
        OperationalSummaryResult summary,
        OperationalTimelineResult timeline
) {
}
