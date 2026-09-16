package com.benhsoan.port.dto.command.vitalsign;

import java.math.BigDecimal;

public record UpdateVitalSignCommand(
        Integer pulse,
        Integer bloodPressureSystolic,
        Integer bloodPressureDiastolic,
        BigDecimal temperature,
        Integer respiratoryRate,
        BigDecimal weight,
        BigDecimal height,
        Integer spo2,
        String note
) {
}
