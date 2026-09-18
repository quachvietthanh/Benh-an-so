package com.benhsoan.port.inbound.specialty;

import java.util.List;

import com.benhsoan.port.dto.result.SpecialtyResult;

public interface SearchSpecialtyUseCase {

    List<SpecialtyResult> search(String keyword, Boolean active);

    default List<SpecialtyResult> search(Boolean active) {
        return search(null, active);
    }
}
