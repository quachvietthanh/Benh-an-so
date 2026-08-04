package com.benhsoan.port.outbound.repository.visit;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
public interface VisitRepository {

    Visit save(Visit visit);

    Optional<Visit> findByVisitCode(String visitCode);

    Optional<Visit> findTopByOrderByVisitCodeDesc();

    Optional<Visit> findById(UUID visitId);

    Optional<Visit> findByIdForUpdate(UUID visitId);

    List<Visit> findByPatientIdOrderByVisitAtDesc(UUID patientId);

    boolean existsByPatientIdAndStatusIn(UUID patientId, Collection<VisitStatus> statuses);

}
