package com.benhsoan.port.dto.command.clinic;

import java.time.LocalTime;

public record UpdateClinicConfigurationCommand(
        String clinicName,
        String address,
        String phone,
        LocalTime openingTime,
        LocalTime closingTime,
        Integer retentionYears
) {
}
