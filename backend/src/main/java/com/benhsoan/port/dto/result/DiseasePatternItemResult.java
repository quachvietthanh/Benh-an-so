package com.benhsoan.port.dto.result;

import java.util.UUID;

public record DiseasePatternItemResult(
        Integer rank,
        UUID catalogId,
        String diseaseCode,
        String diseaseName,
        String diseaseGroup,
        long diagnosisCount,
        double percentage
) {
}
