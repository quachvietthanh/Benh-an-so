package com.benhsoan.port.inbound.appointment;

import java.util.Optional;

import com.benhsoan.port.dto.query.appointment.GetWaitlistSuggestionQuery;
import com.benhsoan.port.dto.result.appointment.WaitlistSuggestionResult;

public interface GetWaitlistSuggestionUseCase {
    Optional<WaitlistSuggestionResult> getSuggestion(GetWaitlistSuggestionQuery query);
}
