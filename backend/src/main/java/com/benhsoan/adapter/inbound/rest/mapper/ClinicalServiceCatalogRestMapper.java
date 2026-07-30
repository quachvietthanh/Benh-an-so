package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.clinical.ClinicalServiceCatalogResponse;
import com.benhsoan.port.dto.result.ClinicalServiceCatalogResult;

@Component
public class ClinicalServiceCatalogRestMapper {

    public ClinicalServiceCatalogResponse toResponse(ClinicalServiceCatalogResult result) {
        return new ClinicalServiceCatalogResponse(
                result.id(), result.serviceCode(), result.serviceName(), result.serviceType(),
                result.resultDataType(), result.unit(), result.referenceRange(), result.description()
        );
    }
}
