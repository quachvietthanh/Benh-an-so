package com.benhsoan.port.inbound.reporting;

import java.time.LocalDate;

import com.benhsoan.port.dto.result.OperationalSummaryResult;

public interface GetOperationalSummaryUseCase {

    OperationalSummaryResult getSummary(LocalDate from, LocalDate to);
}
