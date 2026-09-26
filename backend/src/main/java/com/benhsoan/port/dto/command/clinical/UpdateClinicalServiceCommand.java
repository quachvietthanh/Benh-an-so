package com.benhsoan.port.dto.command.clinical;

import com.benhsoan.domain.clinical.enums.ClinicalResultDataType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;

public record UpdateClinicalServiceCommand(
        String serviceName,
        ClinicalServiceType serviceType,
        ClinicalResultDataType resultDataType,
        String unit,
        String referenceRange,
        String description
) {
}
