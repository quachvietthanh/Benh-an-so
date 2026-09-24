package com.benhsoan.port.dto.command.clinic;

import java.time.LocalTime;

public record UpdateClinicConfigurationCommand(
        String clinicName,
        String address,
        String phone,
        LocalTime openingTime,
        LocalTime closingTime,
        Integer retentionYears,
        Integer signingDeadlineHours,
        Integer activeRecordDurationMonths
) {

    public UpdateClinicConfigurationCommand(
            String clinicName,
            String address,
            String phone,
            LocalTime openingTime,
            LocalTime closingTime,
            Integer retentionYears,
            Integer signingDeadlineHours
    ) {
        this(clinicName, address, phone, openingTime, closingTime, retentionYears, signingDeadlineHours, null);
    }

    public UpdateClinicConfigurationCommand(
            String clinicName,
            String address,
            String phone,
            LocalTime openingTime,
            LocalTime closingTime,
            Integer retentionYears
    ) {
        this(clinicName, address, phone, openingTime, closingTime, retentionYears, null, null);
    }
}
