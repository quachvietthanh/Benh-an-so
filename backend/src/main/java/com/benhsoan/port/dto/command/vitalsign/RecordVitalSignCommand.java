package com.benhsoan.port.dto.command.vitalsign;

import java.math.BigDecimal;
import java.util.UUID;

public record RecordVitalSignCommand(
        UUID visitId,
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
