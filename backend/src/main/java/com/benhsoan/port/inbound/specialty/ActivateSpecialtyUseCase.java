package com.benhsoan.port.inbound.specialty;

import java.util.UUID;

import com.benhsoan.port.dto.result.SpecialtyResult;

public interface ActivateSpecialtyUseCase {

    SpecialtyResult activate(UUID id);
}
