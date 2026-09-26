package com.benhsoan.port.dto.command.clinical;

import java.util.UUID;

import com.benhsoan.domain.clinical.enums.ClinicalResultDataType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;

public record CreateClinicalServiceCommand(
        UUID serviceCatalogId,
        String serviceCode,
        String serviceName,
        ClinicalServiceType serviceType,
        ClinicalResultDataType resultDataType,
        String unit,
        String referenceRange,
        String description
) {
}
