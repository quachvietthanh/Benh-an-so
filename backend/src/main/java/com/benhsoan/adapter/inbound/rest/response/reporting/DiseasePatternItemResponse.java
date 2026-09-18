package com.benhsoan.adapter.inbound.rest.response.reporting;

import java.util.UUID;

public record DiseasePatternItemResponse(
        Integer rank,
        UUID catalogId,
        String diseaseCode,
        String diseaseName,
        String diseaseGroup,
        long diagnosisCount,
        double percentage
) {
}
