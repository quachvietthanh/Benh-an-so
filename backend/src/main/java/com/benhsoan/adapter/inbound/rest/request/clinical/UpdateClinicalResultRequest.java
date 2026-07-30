package com.benhsoan.adapter.inbound.rest.request.clinical;

import java.math.BigDecimal;
import java.util.List;

import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record UpdateClinicalResultRequest(
        BigDecimal numericValue,
        String textValue,
        ClinicalResultAbnormalFlag abnormalFlag,
        String conclusion,
        @NotBlank String changeReason,
        List<@Valid AttachmentMetadataRequest> attachments
) {}
