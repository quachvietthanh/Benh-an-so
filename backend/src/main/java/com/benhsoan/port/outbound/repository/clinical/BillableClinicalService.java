package com.benhsoan.port.outbound.repository.clinical;

import java.util.UUID;

public record BillableClinicalService(
        UUID visitId,
        UUID clinicalOrderItemId,
        UUID serviceCatalogId,
        String serviceName
) {
    public BillableClinicalService(
            UUID clinicalOrderItemId,
            UUID serviceCatalogId,
            String serviceName
    ) {
        this(null, clinicalOrderItemId, serviceCatalogId, serviceName);
    }
}
