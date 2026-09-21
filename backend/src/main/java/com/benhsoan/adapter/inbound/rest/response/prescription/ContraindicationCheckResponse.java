package com.benhsoan.adapter.inbound.rest.response.prescription;

import java.util.List;

public record ContraindicationCheckResponse(
        List<ContraindicationWarningResponse> warnings,
        List<ContraindicationMissingDataResponse> missingData
) {
}
