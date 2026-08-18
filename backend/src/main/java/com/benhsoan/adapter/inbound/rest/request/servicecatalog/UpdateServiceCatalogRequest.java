package com.benhsoan.adapter.inbound.rest.request.servicecatalog;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateServiceCatalogRequest(
        @NotBlank(message = "Service name is required.")
        @Size(max = 255, message = "Service name must not exceed 255 characters.")
        String name,

        @NotNull(message = "Service active status is required.")
        Boolean active,

        @NotNull(message = "Service price is required.")
        @PositiveOrZero(message = "Service price must be greater than or equal to 0.")
        BigDecimal price,

        @NotNull(message = "Price effective date is required.")
        LocalDate effectiveFrom
) {
}
