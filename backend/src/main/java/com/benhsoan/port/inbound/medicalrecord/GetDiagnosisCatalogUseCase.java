package com.benhsoan.port.inbound.medicalrecord;

import java.util.List;

import com.benhsoan.port.dto.result.DiagnosisCatalogResult;

public interface GetDiagnosisCatalogUseCase {

    List<DiagnosisCatalogResult> search(String query);
}
