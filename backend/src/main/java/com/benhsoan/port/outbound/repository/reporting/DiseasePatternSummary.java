package com.benhsoan.port.outbound.repository.reporting;

import java.util.UUID;

public record DiseasePatternSummary(
        UUID diagnosisCatalogId,
        String diseaseCode,
        String diseaseName,
        String diseaseGroup,
        long diagnosisCount
) {
}
