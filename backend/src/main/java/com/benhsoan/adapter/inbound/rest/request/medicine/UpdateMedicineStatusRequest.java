package com.benhsoan.adapter.inbound.rest.request.medicine;

import jakarta.validation.constraints.NotNull;

public record UpdateMedicineStatusRequest(
        @NotNull(message = "Medicine active status is required.")
        Boolean active
) {
}
