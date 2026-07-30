package com.benhsoan.port.outbound.repository.crudRepository.visit;

import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.visit.Visit;

public interface VisitRepository {

    Optional<Visit> findById(UUID visitId);
}
