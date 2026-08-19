package com.benhsoan.port.dto.result.clinic;

import java.time.LocalTime;

public record ClinicConfigurationResult(
        String clinicName,
        String address,
        String phone,
        LocalTime openingTime,
        LocalTime closingTime
) {

    public static ClinicConfigurationResult empty() {
        return new ClinicConfigurationResult(null, null, null, null, null);
    }
}
