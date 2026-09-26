package com.benhsoan.port.inbound.medicalrecord;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.DiagnosisCatalogResult;

public interface DiagnosisCatalogManagementQueryUseCase {

    List<DiagnosisCatalogResult> search(String keyword, Boolean active);

    DiagnosisCatalogResult getById(UUID diagnosisCatalogId);
}
