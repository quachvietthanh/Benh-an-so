package com.benhsoan.port.dto.command.clinical;

import java.math.BigDecimal;
import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;
import com.benhsoan.domain.shared.exception.ValidationException;

public record UpdateClinicalResultCommand(
        BigDecimal numericValue,
        String textValue,
        ClinicalResultAbnormalFlag abnormalFlag,
        String conclusion,
        String changeReason
) {
    public UpdateClinicalResultCommand {
        if (changeReason == null || changeReason.isBlank()) {
            throw new ValidationException("Change reason is required.");
        }
    }
}
