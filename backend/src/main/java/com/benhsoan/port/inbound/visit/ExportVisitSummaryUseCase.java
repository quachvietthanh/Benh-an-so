package com.benhsoan.port.inbound.visit;

import java.util.UUID;

import com.benhsoan.port.dto.result.VisitSummaryPrintResult;

public interface ExportVisitSummaryUseCase {

    VisitSummaryPrintResult export(UUID visitId);
}
