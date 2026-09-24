package com.benhsoan.port.dto.result.portal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PatientPortalClinicalResultSummaryResult(
        UUID clinicalResultId,
        UUID clinicalOrderItemId,
        UUID visitId,
        String serviceCode,
        String serviceName,
        String resultType,
        BigDecimal numericValue,
        BigDecimal lowerBound,
        BigDecimal upperBound,
        String textValue,
        String unit,
        String referenceRange,
        String abnormalFlag,
        String conclusion,
        String status,
        String doctorName,
        Instant enteredAt,
        boolean hasAttachment
) {
}
