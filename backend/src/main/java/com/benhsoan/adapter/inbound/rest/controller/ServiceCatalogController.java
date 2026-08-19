package com.benhsoan.adapter.inbound.rest.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.benhsoan.adapter.inbound.rest.mapper.ServiceCatalogRestMapper;
import com.benhsoan.adapter.inbound.rest.request.servicecatalog.CreateServiceCatalogRequest;
import com.benhsoan.adapter.inbound.rest.request.servicecatalog.UpdateServiceCatalogRequest;
import com.benhsoan.adapter.inbound.rest.request.servicecatalog.UpdateServiceCatalogStatusRequest;
import com.benhsoan.adapter.inbound.rest.response.servicecatalog.ServiceCatalogResponse;
import com.benhsoan.adapter.inbound.rest.response.servicecatalog.ServicePriceResponse;
import com.benhsoan.domain.auth.enums.Permission;
import com.benhsoan.infrastructure.security.annotation.CheckPermission;
import com.benhsoan.infrastructure.security.annotation.CheckPermission.Operator;
import com.benhsoan.port.dto.command.servicecatalog.SearchServiceCatalogQuery;
import com.benhsoan.port.inbound.servicecatalog.CreateServiceCatalogUseCase;
import com.benhsoan.port.inbound.servicecatalog.GetServiceCatalogUseCase;
import com.benhsoan.port.inbound.servicecatalog.GetServicePriceHistoryUseCase;
import com.benhsoan.port.inbound.servicecatalog.SearchServiceCatalogUseCase;
import com.benhsoan.port.inbound.servicecatalog.UpdateServiceCatalogStatusUseCase;
import com.benhsoan.port.inbound.servicecatalog.UpdateServiceCatalogUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/services")
public class ServiceCatalogController {

    private final CreateServiceCatalogUseCase createServiceCatalogUseCase;
    private final UpdateServiceCatalogUseCase updateServiceCatalogUseCase;
    private final UpdateServiceCatalogStatusUseCase updateServiceCatalogStatusUseCase;
    private final GetServiceCatalogUseCase getServiceCatalogUseCase;
    private final SearchServiceCatalogUseCase searchServiceCatalogUseCase;
    private final GetServicePriceHistoryUseCase getServicePriceHistoryUseCase;
    private final ServiceCatalogRestMapper mapper;

    @GetMapping
    @CheckPermission(Permission.SERVICE_CATALOG_READ)
    public Page<ServiceCatalogResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = {"serviceName", "serviceCode"}) Pageable pageable
    ) {
        return mapper.toResponse(searchServiceCatalogUseCase.search(
                new SearchServiceCatalogQuery(keyword, active, pageable)
        ));
    }

    @GetMapping("/{serviceCatalogId}")
    @CheckPermission(Permission.SERVICE_CATALOG_READ)
    public ServiceCatalogResponse getById(@PathVariable UUID serviceCatalogId) {
        return mapper.toResponse(getServiceCatalogUseCase.getById(serviceCatalogId));
    }

    @PostMapping
    @CheckPermission(
            value = {Permission.SERVICE_CATALOG_CREATE, Permission.SERVICE_PRICE_MANAGE},
            operator = Operator.ALL
    )
    public ResponseEntity<ServiceCatalogResponse> create(
            @Valid @RequestBody CreateServiceCatalogRequest request
    ) {
        ServiceCatalogResponse response = mapper.toResponse(
                createServiceCatalogUseCase.create(mapper.toCommand(request))
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{serviceCatalogId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{serviceCatalogId}")
    @CheckPermission(
            value = {Permission.SERVICE_CATALOG_UPDATE, Permission.SERVICE_PRICE_MANAGE},
            operator = Operator.ALL
    )
    public ServiceCatalogResponse update(
            @PathVariable UUID serviceCatalogId,
            @Valid @RequestBody UpdateServiceCatalogRequest request
    ) {
        return mapper.toResponse(updateServiceCatalogUseCase.update(
                mapper.toCommand(serviceCatalogId, request)
        ));
    }

    @PatchMapping("/{serviceCatalogId}/status")
    @CheckPermission(Permission.SERVICE_CATALOG_UPDATE)
    public ServiceCatalogResponse updateStatus(
            @PathVariable UUID serviceCatalogId,
            @Valid @RequestBody UpdateServiceCatalogStatusRequest request
    ) {
        return mapper.toResponse(updateServiceCatalogStatusUseCase.updateStatus(
                serviceCatalogId,
                request.active()
        ));
    }

    @GetMapping("/{serviceCatalogId}/prices")
    @CheckPermission(Permission.SERVICE_CATALOG_READ)
    public List<ServicePriceResponse> getPriceHistory(@PathVariable UUID serviceCatalogId) {
        return mapper.toPriceResponse(getServicePriceHistoryUseCase.getHistory(serviceCatalogId));
    }
}
