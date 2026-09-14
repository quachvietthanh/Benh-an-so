package com.benhsoan.adapter.inbound.rest.response.medicalrecord;

import java.util.List;

public record DiagnosisSuggestionResponse(
        List<DiagnosisCatalogResponse> recent,
        List<DiagnosisCatalogResponse> popular,
        List<String> diseaseGroups
) {
}
