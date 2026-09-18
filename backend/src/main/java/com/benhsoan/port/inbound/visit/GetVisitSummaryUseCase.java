package com.benhsoan.port.inbound.visit;

import java.util.UUID;

import com.benhsoan.port.dto.result.VisitSummaryResult;

public interface GetVisitSummaryUseCase {

    VisitSummaryResult getSummary(UUID visitId);
}
