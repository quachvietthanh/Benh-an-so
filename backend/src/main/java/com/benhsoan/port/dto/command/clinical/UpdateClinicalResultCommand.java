package com.benhsoan.port.dto.command.clinical;

import java.math.BigDecimal;
import java.util.List;

import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;
import com.benhsoan.domain.shared.exception.ValidationException;

public record UpdateClinicalResultCommand(
        BigDecimal numericValue,
        String textValue,
        ClinicalResultAbnormalFlag abnormalFlag,
        String conclusion,
        String changeReason,
        List<AttachmentMetadataCommand> attachments
) {
    public UpdateClinicalResultCommand {
        if (changeReason == null || changeReason.isBlank()) {
            throw new ValidationException("Change reason is required.");
        }
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }
}
