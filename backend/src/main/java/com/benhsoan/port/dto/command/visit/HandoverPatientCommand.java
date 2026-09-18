package com.benhsoan.port.dto.command.visit;

import java.util.UUID;

public record HandoverPatientCommand(
        UUID targetDoctorId,
        String reason
) {
}
