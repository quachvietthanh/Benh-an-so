package com.benhsoan.adapter.inbound.rest.request.clinical;

import java.math.BigDecimal;
import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;

public record EnterClinicalResultRequest(
        BigDecimal numericValue,
        String textValue,
        ClinicalResultAbnormalFlag abnormalFlag,
        String conclusion
) {}
