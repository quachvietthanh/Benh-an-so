package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.clinical.enums.ClinicalResultDataType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;

public record ClinicalServiceManagementResult(
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
        List<ClinicalReferenceRangeResult> referenceRanges
) {
}
