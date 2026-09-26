package com.benhsoan.port.inbound.carelog;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.PostCareLogResult;

public interface GetPatientCareLogsUseCase {

    List<PostCareLogResult> getForPatient(UUID patientId);
}
