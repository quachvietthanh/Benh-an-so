package com.benhsoan.port.dto.command.clinical;

import java.math.BigDecimal;
import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;

public record EnterClinicalResultCommand(
        BigDecimal numericValue,
        String textValue,
        ClinicalResultAbnormalFlag abnormalFlag,
        String conclusion
) {}
