package com.benhsoan.port.inbound.reporting;

import java.time.LocalDate;

import com.benhsoan.port.dto.result.OperationalTimelineResult;

public interface GetOperationalTimelineUseCase {

    OperationalTimelineResult getTimeline(LocalDate from, LocalDate to);
}
