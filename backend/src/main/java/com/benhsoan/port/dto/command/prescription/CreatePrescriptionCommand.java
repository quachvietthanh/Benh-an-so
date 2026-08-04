package com.benhsoan.port.dto.command.prescription;

import java.util.List;
import java.util.UUID;

import lombok.Builder;

@Builder
public record CreatePrescriptionCommand(

        UUID medicalRecordId,

        String note,

        List<CreatePrescriptionItemCommand> items,

        List<PrescriptionInteractionOverrideCommand> interactionOverrides

) {
}
