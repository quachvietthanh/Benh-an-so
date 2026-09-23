package com.benhsoan.port.inbound.inventory;

import java.time.LocalDate;

import com.benhsoan.port.dto.result.ProcurementSuggestionResult;

public interface GetMedicationProcurementSuggestionUseCase {

    ProcurementSuggestionResult getSuggestions(LocalDate from, LocalDate to, boolean onlyBelowThreshold);
}
