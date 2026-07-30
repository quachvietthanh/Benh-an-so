package com.benhsoan.port.outbound.repository.crudRepository.visit;

import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.visit.Visit;
import com.benhsoan.port.outbound.repository.BaseRepository;

public interface VisitRepository extends BaseRepository<Visit, UUID> {

    Optional<Visit> findByVisitCode(String visitCode);
}
