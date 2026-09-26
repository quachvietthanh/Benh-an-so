package com.benhsoan.adapter.inbound.rest.request.servicecatalog;

import jakarta.validation.constraints.NotNull;

public record UpdateServiceCatalogStatusRequest(
        @NotNull(message = "Service active status is required.")
        Boolean active
) {
}
