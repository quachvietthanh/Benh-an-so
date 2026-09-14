package com.benhsoan.port.inbound.medicalrecord;

import com.benhsoan.port.dto.result.DiagnosisSuggestionResult;

public interface GetDiagnosisSuggestionsUseCase {

    DiagnosisSuggestionResult suggest();
}
