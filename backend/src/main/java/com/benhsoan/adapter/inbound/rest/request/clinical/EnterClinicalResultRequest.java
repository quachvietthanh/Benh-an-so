package com.benhsoan.adapter.inbound.rest.request.clinical;

import java.math.BigDecimal;
import java.util.List;

import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;

import jakarta.validation.Valid;

public record EnterClinicalResultRequest(
        BigDecimal numericValue,
        String textValue,
        ClinicalResultAbnormalFlag abnormalFlag,
        String conclusion,
        List<@Valid AttachmentMetadataRequest> attachments
) {}
