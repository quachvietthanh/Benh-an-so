package com.benhsoan.adapter.inbound.rest.request.clinic;

import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateClinicConfigurationRequest(
        @NotBlank(message = "Clinic name is required.")
        @Size(max = 150, message = "Clinic name must not exceed 150 characters.")
        String clinicName,

        @Size(max = 500, message = "Address must not exceed 500 characters.")
        String address,

        @Size(max = 30, message = "Phone must not exceed 30 characters.")
        String phone,

        @NotNull(message = "Opening time is required.")
        LocalTime openingTime,

        @NotNull(message = "Closing time is required.")
        LocalTime closingTime
) {
}
