package com.benhsoan.adapter.inbound.rest.response.clinical;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.clinical.enums.ClinicalResultDataType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;

public record ClinicalServiceManagementResponse(
        UUID id,
        UUID serviceCatalogId,
        String serviceCode,
        String serviceName,
        ClinicalServiceType serviceType,
        ClinicalResultDataType resultDataType,
        String unit,
        String referenceRange,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        List<ClinicalReferenceRangeResponse> referenceRanges
) {
}
