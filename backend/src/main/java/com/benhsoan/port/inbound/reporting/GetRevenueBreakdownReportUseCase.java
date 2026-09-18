package com.benhsoan.port.inbound.reporting;

import java.time.LocalDate;

import com.benhsoan.port.dto.result.RevenueBreakdownReportResult;

public interface GetRevenueBreakdownReportUseCase {

    RevenueBreakdownReportResult getRevenueBreakdown(LocalDate from, LocalDate to);
}
