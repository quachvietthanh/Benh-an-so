package com.benhsoan.port.outbound.repository.clinical;

import java.util.UUID;

public record BillableClinicalService(
        UUID clinicalOrderItemId,
        UUID serviceCatalogId,
        String serviceName
) {
}
