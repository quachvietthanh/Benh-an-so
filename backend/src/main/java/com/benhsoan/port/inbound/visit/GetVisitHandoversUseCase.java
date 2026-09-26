package com.benhsoan.port.inbound.visit;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.VisitHandoverResult;

public interface GetVisitHandoversUseCase {

    List<VisitHandoverResult> getHandovers(UUID visitId);
}
