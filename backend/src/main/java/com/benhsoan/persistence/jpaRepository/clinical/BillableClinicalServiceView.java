package com.benhsoan.persistence.jpaRepository.clinical;

import java.util.UUID;

public interface BillableClinicalServiceView {

    default UUID getVisitId() {
        return null;
    }

    UUID getClinicalOrderItemId();

    UUID getServiceCatalogId();

    String getServiceName();
}
