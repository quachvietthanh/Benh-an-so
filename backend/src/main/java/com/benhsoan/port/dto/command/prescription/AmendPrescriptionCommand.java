package com.benhsoan.port.dto.command.prescription;

import java.util.List;
import java.util.UUID;

import lombok.Builder;

@Builder
public record AmendPrescriptionCommand(

        UUID prescriptionId,

        String note,

        String changeReason,

        List<AmendPrescriptionItemCommand> items,

        List<PrescriptionInteractionOverrideCommand> interactionOverrides

) {
}
