package com.benhsoan.port.inbound.billing;

import com.benhsoan.port.dto.result.CurrentShiftSummaryResult;

public interface GetCurrentShiftSummaryUseCase {

    CurrentShiftSummaryResult getCurrentSummary();
}
