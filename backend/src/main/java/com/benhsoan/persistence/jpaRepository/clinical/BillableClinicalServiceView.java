package com.benhsoan.persistence.jpaRepository.clinical;

import java.util.UUID;

public interface BillableClinicalServiceView {

    UUID getClinicalOrderItemId();

    UUID getServiceCatalogId();

    String getServiceName();
}
