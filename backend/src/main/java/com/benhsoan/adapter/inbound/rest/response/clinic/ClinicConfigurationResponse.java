package com.benhsoan.adapter.inbound.rest.response.clinic;

import java.time.LocalTime;

public record ClinicConfigurationResponse(
        String clinicName,
        String address,
        String phone,
        LocalTime openingTime,
        LocalTime closingTime,
        Integer retentionYears
) {
}
