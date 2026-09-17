package com.benhsoan.port.inbound.vitalsign;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.benhsoan.port.dto.result.vitalsign.VitalSignResult;

public interface GetVitalSignUseCase {

    VitalSignResult getById(UUID id);

    Optional<VitalSignResult> getLatestByVisitId(UUID visitId);

    List<VitalSignResult> getByVisitId(UUID visitId);
}
