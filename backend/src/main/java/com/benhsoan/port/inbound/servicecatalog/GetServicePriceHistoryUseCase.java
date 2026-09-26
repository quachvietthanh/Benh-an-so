package com.benhsoan.port.inbound.servicecatalog;

import java.util.List;
import java.util.UUID;

import com.benhsoan.port.dto.result.servicecatalog.ServicePriceResult;

public interface GetServicePriceHistoryUseCase {

    List<ServicePriceResult> getHistory(UUID serviceCatalogId);
}
