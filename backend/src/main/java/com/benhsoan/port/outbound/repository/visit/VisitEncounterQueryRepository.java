package com.benhsoan.port.outbound.repository.queryRepository.visit;

import java.util.Optional;
import java.util.UUID;

import com.benhsoan.port.dto.result.VisitEncounterResult;

public interface VisitEncounterQueryRepository {

    Optional<VisitEncounterResult> findByVisitId(UUID visitId);
}
