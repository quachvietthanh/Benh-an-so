package com.benhsoan.port.dto.command.patient;

import java.util.UUID;

import lombok.Builder;

@Builder
public record MergePatientsCommand(
        UUID sourcePatientId,
        UUID targetPatientId,
        String reason
) {
}
