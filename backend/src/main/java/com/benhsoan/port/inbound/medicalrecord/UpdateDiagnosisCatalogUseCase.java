package com.benhsoan.port.inbound.medicalrecord;

import com.benhsoan.port.dto.command.medicalrecord.UpdateDiagnosisCatalogCommand;
import com.benhsoan.port.dto.result.DiagnosisCatalogResult;

public interface UpdateDiagnosisCatalogUseCase {

    DiagnosisCatalogResult update(UpdateDiagnosisCatalogCommand command);
}
