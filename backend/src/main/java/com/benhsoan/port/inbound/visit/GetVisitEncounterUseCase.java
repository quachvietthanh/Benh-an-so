package com.benhsoan.port.inbound.visit;

import java.util.UUID;

import com.benhsoan.port.dto.result.VisitEncounterResult;

public interface GetVisitEncounterUseCase {

    VisitEncounterResult getEncounter(UUID visitId);
}
