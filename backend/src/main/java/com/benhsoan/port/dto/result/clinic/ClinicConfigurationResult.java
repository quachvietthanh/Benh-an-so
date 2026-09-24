package com.benhsoan.port.dto.result.clinic;

import java.time.LocalTime;

public record ClinicConfigurationResult(
        String clinicName,
        String address,
        String phone,
        LocalTime openingTime,
        LocalTime closingTime,
        Integer retentionYears,
        Integer signingDeadlineHours,
        Integer activeRecordDurationMonths
) {

    public ClinicConfigurationResult(
            String clinicName,
            String address,
            String phone,
            LocalTime openingTime,
            LocalTime closingTime,
            Integer retentionYears,
            Integer signingDeadlineHours
    ) {
        this(clinicName, address, phone, openingTime, closingTime, retentionYears, signingDeadlineHours, 12);
    }

    public ClinicConfigurationResult(
            String clinicName,
            String address,
            String phone,
            LocalTime openingTime,
            LocalTime closingTime,
            Integer retentionYears
    ) {
        this(clinicName, address, phone, openingTime, closingTime, retentionYears, 24, 12);
    }

    public static ClinicConfigurationResult empty() {
        return new ClinicConfigurationResult(null, null, null, null, null, null, null, null);
    }
}
