package com.benhsoan.port.dto.result.portal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PatientPortalClinicalResultDetailResult(
        UUID clinicalResultId,
        UUID clinicalOrderItemId,
        UUID visitId,
        String visitCode,
        Instant visitAt,
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
        String orderingDoctorName,
        String performingDoctorName,
        String specialtyName,
        Instant enteredAt,
        List<AttachmentView> attachments
) {
    public record AttachmentView(
            UUID attachmentId,
            String fileName,
            String contentType,
            long fileSize,
            String attachmentType
    ) {
    }
}
