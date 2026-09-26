package com.benhsoan.port.dto.command.prescription;

import java.util.UUID;

import lombok.Builder;

@Builder
public record PrescriptionInteractionOverrideCommand(

        UUID ruleId,

        String overrideReason

) {
}
