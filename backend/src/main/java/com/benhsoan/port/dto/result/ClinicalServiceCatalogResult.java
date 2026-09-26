package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.clinical.enums.ClinicalResultDataType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;

public record ClinicalServiceCatalogResult(
        UUID id,
        String serviceCode,
        String serviceName,
        ClinicalServiceType serviceType,
        ClinicalResultDataType resultDataType,
        String unit,
        String referenceRange,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
