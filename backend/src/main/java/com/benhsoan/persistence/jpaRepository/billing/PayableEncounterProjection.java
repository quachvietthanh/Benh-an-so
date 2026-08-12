package com.benhsoan.persistence.jpaRepository.billing;

import java.time.Instant;
import java.util.UUID;

public interface PayableEncounterProjection {

    UUID getVisitId();

    String getVisitCode();

    UUID getPatientId();

    String getPatientCode();

    String getPatientName();

    String getReason();

    Instant getCompletedAt();
}
