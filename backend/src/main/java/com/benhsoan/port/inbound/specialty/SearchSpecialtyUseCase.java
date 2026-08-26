package com.benhsoan.port.inbound.specialty;

import java.util.List;

import com.benhsoan.port.dto.result.SpecialtyResult;

public interface SearchSpecialtyUseCase {

    List<SpecialtyResult> search(Boolean active);
}
