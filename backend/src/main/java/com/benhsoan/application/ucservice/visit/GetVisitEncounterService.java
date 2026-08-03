package com.benhsoan.application.ucservice.visit;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.result.VisitEncounterResult;
import com.benhsoan.port.inbound.visit.GetVisitEncounterUseCase;
import com.benhsoan.port.outbound.repository.queryRepository.visit.VisitEncounterQueryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetVisitEncounterService implements GetVisitEncounterUseCase {

    private final VisitEncounterQueryRepository visitEncounterQueryRepository;
    private final VisitEncounterAuthorization authorization;

    @Override
    public VisitEncounterResult getEncounter(UUID visitId) {
        VisitEncounterResult encounter = visitEncounterQueryRepository.findByVisitId(visitId)
                .orElseThrow(() -> new VisitNotFoundException(visitId));
        authorization.requireReadAccess(encounter.doctor().id());
        return encounter;
    }
}
