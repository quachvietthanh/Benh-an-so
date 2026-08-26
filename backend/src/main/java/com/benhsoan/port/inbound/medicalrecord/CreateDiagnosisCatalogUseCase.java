package com.benhsoan.port.inbound.medicalrecord;

import com.benhsoan.port.dto.command.medicalrecord.CreateDiagnosisCatalogCommand;
import com.benhsoan.port.dto.result.DiagnosisCatalogResult;

public interface CreateDiagnosisCatalogUseCase {

    DiagnosisCatalogResult create(CreateDiagnosisCatalogCommand command);
}
