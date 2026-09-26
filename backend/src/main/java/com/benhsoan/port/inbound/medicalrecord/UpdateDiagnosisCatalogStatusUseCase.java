package com.benhsoan.port.inbound.medicalrecord;

import java.util.UUID;

import com.benhsoan.port.dto.result.DiagnosisCatalogResult;

public interface UpdateDiagnosisCatalogStatusUseCase {

    DiagnosisCatalogResult updateStatus(UUID diagnosisCatalogId, boolean active);
}
