package com.benhsoan.port.dto.result;

import java.util.List;

public record ContraindicationCheckResult(
        List<ContraindicationWarningResult> warnings,
        List<ContraindicationMissingDataResult> missingData
) {
}
