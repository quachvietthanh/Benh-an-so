package com.benhsoan.port.inbound.prescription;

import java.util.UUID;

import com.benhsoan.port.dto.result.DispenseSuggestionResult;

public interface GetDispenseSuggestionUseCase {

    DispenseSuggestionResult getSuggestion(UUID prescriptionId);
}
