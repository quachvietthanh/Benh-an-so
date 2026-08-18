package com.benhsoan.application.ucservice.billing;

import java.math.BigDecimal;
import java.util.UUID;

record ClinicalServiceCharge(
        UUID clinicalOrderItemId,
        String serviceName,
        BigDecimal price
) {
}
