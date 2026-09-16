package com.benhsoan.port.inbound.prescription;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.PrescriptionDispenseHistoryResult;

public interface GetPrescriptionDispenseHistoryUseCase {

    List<PrescriptionDispenseHistoryResult> getHistory(UUID prescriptionId);
}