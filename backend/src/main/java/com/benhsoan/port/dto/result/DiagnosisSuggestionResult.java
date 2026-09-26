package com.benhsoan.port.dto.result;

import java.util.List;

public record DiagnosisSuggestionResult(
        List<DiagnosisCatalogResult> recent,
        List<DiagnosisCatalogResult> popular,
        List<String> diseaseGroups
) {
}
