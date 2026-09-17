package com.benhsoan.port.inbound.vitalsign;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.vitalsign.VitalSignResult;

public interface GetPatientVitalSignHistoryUseCase {

    List<VitalSignResult> getHistory(UUID patientId);
}
