package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.servicecatalog.CreateServiceCatalogRequest;
import com.benhsoan.adapter.inbound.rest.request.servicecatalog.UpdateServiceCatalogRequest;
import com.benhsoan.adapter.inbound.rest.response.servicecatalog.ServiceCatalogResponse;
import com.benhsoan.adapter.inbound.rest.response.servicecatalog.ServicePriceResponse;
import com.benhsoan.port.dto.command.servicecatalog.CreateServiceCatalogCommand;
import com.benhsoan.port.dto.command.servicecatalog.UpdateServiceCatalogCommand;
import com.benhsoan.port.dto.result.servicecatalog.ServiceCatalogResult;
import com.benhsoan.port.dto.result.servicecatalog.ServicePriceResult;

@Component
public class ServiceCatalogRestMapper {

    public CreateServiceCatalogCommand toCommand(CreateServiceCatalogRequest request) {
        return new CreateServiceCatalogCommand(
                request.serviceCode(),
                request.name(),
                request.price(),
                request.effectiveFrom()
        );
    }

    public UpdateServiceCatalogCommand toCommand(
            UUID serviceCatalogId,
            UpdateServiceCatalogRequest request
    ) {
        return new UpdateServiceCatalogCommand(
                serviceCatalogId,
                request.name(),
                request.active(),
                request.price(),
                request.effectiveFrom()
        );
    }

    public ServiceCatalogResponse toResponse(ServiceCatalogResult result) {
        return new ServiceCatalogResponse(
                result.id(),
                result.serviceCode(),
                result.serviceName(),
                result.price(),
                result.effectiveFrom(),
                result.active()
        );
    }

    public Page<ServiceCatalogResponse> toResponse(Page<ServiceCatalogResult> resultPage) {
        return resultPage.map(this::toResponse);
    }

    public List<ServicePriceResponse> toPriceResponse(List<ServicePriceResult> results) {
        return results.stream().map(this::toPriceResponse).toList();
    }

    private ServicePriceResponse toPriceResponse(ServicePriceResult result) {
        return new ServicePriceResponse(
                result.id(),
                result.price(),
                result.effectiveFrom(),
                result.createdAt(),
                result.createdBy()
        );
    }
}
