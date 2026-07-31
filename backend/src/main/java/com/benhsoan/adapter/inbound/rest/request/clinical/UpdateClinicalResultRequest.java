package com.benhsoan.adapter.inbound.rest.request.clinical;

import java.math.BigDecimal;
import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;
import jakarta.validation.constraints.NotBlank;

public record UpdateClinicalResultRequest(
        BigDecimal numericValue,
        String textValue,
        ClinicalResultAbnormalFlag abnormalFlag,
        String conclusion,
        @NotBlank String changeReason
) {}
