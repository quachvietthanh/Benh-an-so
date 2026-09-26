package com.benhsoan.port.inbound.specialty;

import com.benhsoan.port.dto.command.specialty.CreateSpecialtyCommand;
import com.benhsoan.port.dto.result.specialty.SpecialtyDetailResult;

public interface CreateSpecialtyUseCase {

    SpecialtyDetailResult create(CreateSpecialtyCommand command);
}
