package com.benhsoan.adapter.inbound.rest.response.clinical;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;
import com.benhsoan.domain.clinical.enums.ClinicalResultStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultType;
import com.benhsoan.domain.clinical.enums.MedicalAttachmentType;

public record ClinicalResultResponse(
        UUID id,
        UUID clinicalOrderItemId,
        UUID visitId,
        ClinicalResultType resultType,
        BigDecimal numericValue,
        String textValue,
        String unit,
        String referenceRange,
        ClinicalResultAbnormalFlag abnormalFlag,
        String conclusion,
        ClinicalResultStatus status,
        List<AttachmentResponse> attachments,
        List<HistoryResponse> histories
) {
    public record AttachmentResponse(UUID id, String fileName, String contentType, long fileSize,
            MedicalAttachmentType attachmentType) {}

    public record HistoryResponse(
            UUID id,
            ClinicalResultType oldResultType,
            ClinicalResultType newResultType,
            BigDecimal oldNumericValue,
            BigDecimal newNumericValue,
            String oldTextValue,
            String newTextValue,
            String oldUnit,
            String newUnit,
            String oldReferenceRange,
            String newReferenceRange,
            ClinicalResultAbnormalFlag oldAbnormalFlag,
            ClinicalResultAbnormalFlag newAbnormalFlag,
            String oldConclusion,
            String newConclusion,
            ClinicalResultStatus oldStatus,
            ClinicalResultStatus newStatus,
            String changeReason,
            UUID changedBy,
            Instant changedAt
    ) {}
}
