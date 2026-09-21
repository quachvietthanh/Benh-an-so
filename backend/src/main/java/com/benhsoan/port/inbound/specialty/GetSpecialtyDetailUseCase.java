package com.benhsoan.port.inbound.specialty;

import java.util.UUID;

import com.benhsoan.port.dto.result.specialty.SpecialtyDetailResult;

public interface GetSpecialtyDetailUseCase {

    SpecialtyDetailResult getById(UUID id);
}
