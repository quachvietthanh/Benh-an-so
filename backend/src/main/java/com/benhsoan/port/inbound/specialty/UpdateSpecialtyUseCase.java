package com.benhsoan.port.inbound.specialty;

import com.benhsoan.port.dto.command.specialty.UpdateSpecialtyCommand;
import com.benhsoan.port.dto.result.specialty.SpecialtyDetailResult;

public interface UpdateSpecialtyUseCase {

    SpecialtyDetailResult update(UpdateSpecialtyCommand command);
}
