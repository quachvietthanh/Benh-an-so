package com.benhsoan.port.inbound.visit;

import java.util.UUID;

import com.benhsoan.port.dto.command.visit.HandoverPatientCommand;
import com.benhsoan.port.dto.result.VisitHandoverResult;

public interface HandoverPatientUseCase {

    VisitHandoverResult handover(UUID visitId, HandoverPatientCommand command);
}
