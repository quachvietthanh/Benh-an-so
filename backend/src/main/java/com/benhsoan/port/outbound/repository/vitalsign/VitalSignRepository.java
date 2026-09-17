package com.benhsoan.port.outbound.repository.vitalsign;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.domain.vitalsign.VitalSign;

public interface VitalSignRepository {

    VitalSign save(VitalSign vitalSign);

    Optional<VitalSign> findById(UUID id);

    Optional<VitalSign> findLatestByVisitId(UUID visitId);

    List<VitalSign> findByVisitId(UUID visitId);

    List<VitalSign> findHistoryByPatientId(UUID patientId);

    boolean existsByVisitId(UUID visitId);
}
