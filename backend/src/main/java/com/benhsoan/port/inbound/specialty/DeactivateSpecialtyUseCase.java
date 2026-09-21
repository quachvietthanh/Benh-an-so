package com.benhsoan.port.inbound.specialty;

import com.benhsoan.port.dto.command.specialty.DeactivateSpecialtyCommand;
import com.benhsoan.port.dto.result.SpecialtyResult;

public interface DeactivateSpecialtyUseCase {

    SpecialtyResult deactivate(DeactivateSpecialtyCommand command);
}
