package com.benhsoan.adapter.inbound.rest.response.clinical;

import java.util.UUID;

import com.benhsoan.domain.clinical.enums.ClinicalResultDataType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;

public record ClinicalServiceCatalogResponse(
        UUID id,
        String serviceCode,
        String serviceName,
        ClinicalServiceType serviceType,
        ClinicalResultDataType resultDataType,
        String unit,
        String referenceRange,
        String description
) {
}
