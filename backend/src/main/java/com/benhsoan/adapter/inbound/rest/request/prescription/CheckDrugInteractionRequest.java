package com.benhsoan.adapter.inbound.rest.request.prescription;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;

public record CheckDrugInteractionRequest(

        @NotEmpty
        List<UUID> drugIds

) {
}
