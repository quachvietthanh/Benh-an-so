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

import com.benhsoan.adapter.inbound.rest.mapper.ClinicalServiceCatalogManagementRestMapper;
import com.benhsoan.adapter.inbound.rest.request.clinical.ClinicalReferenceRangeRequest;
import com.benhsoan.adapter.inbound.rest.request.clinical.CreateClinicalServiceRequest;
import com.benhsoan.adapter.inbound.rest.request.clinical.UpdateClinicalServiceRequest;
import com.benhsoan.adapter.inbound.rest.request.clinical.UpdateClinicalServiceStatusRequest;
import com.benhsoan.adapter.inbound.rest.response.clinical.ClinicalReferenceRangeResponse;
import com.benhsoan.adapter.inbound.rest.response.clinical.ClinicalServiceManagementResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.inbound.clinical.CreateClinicalReferenceRangeUseCase;
import com.benhsoan.port.inbound.clinical.CreateClinicalServiceUseCase;
import com.benhsoan.port.inbound.clinical.GetClinicalReferenceRangesUseCase;
import com.benhsoan.port.inbound.clinical.GetClinicalServicesUseCase;
import com.benhsoan.port.inbound.clinical.UpdateClinicalReferenceRangeStatusUseCase;
import com.benhsoan.port.inbound.clinical.UpdateClinicalReferenceRangeUseCase;
import com.benhsoan.port.inbound.clinical.UpdateClinicalServiceStatusUseCase;
import com.benhsoan.port.inbound.clinical.UpdateClinicalServiceUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/clinical-services")
public class ClinicalServiceCatalogManagementController {

    private static final String MANAGE_PERMISSION = "CLINICAL_SERVICE_MANAGE";

    private final GetClinicalServicesUseCase getClinicalServicesUseCase;
    private final CreateClinicalServiceUseCase createClinicalServiceUseCase;
    private final UpdateClinicalServiceUseCase updateClinicalServiceUseCase;
    private final UpdateClinicalServiceStatusUseCase updateClinicalServiceStatusUseCase;
    private final GetClinicalReferenceRangesUseCase getClinicalReferenceRangesUseCase;
    private final CreateClinicalReferenceRangeUseCase createClinicalReferenceRangeUseCase;
    private final UpdateClinicalReferenceRangeUseCase updateClinicalReferenceRangeUseCase;
    private final UpdateClinicalReferenceRangeStatusUseCase updateClinicalReferenceRangeStatusUseCase;
    private final ClinicalServiceCatalogManagementRestMapper mapper;

    @GetMapping
    @RequirePermission(MANAGE_PERMISSION)
    public Page<ClinicalServiceManagementResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = {"serviceName", "serviceCode"}) Pageable pageable
    ) {
        return getClinicalServicesUseCase.search(keyword, active, pageable).map(mapper::toResponse);
    }
    @GetMapping("/{clinicalServiceId}")
    @RequirePermission(MANAGE_PERMISSION)
    public ClinicalServiceManagementResponse getById(@PathVariable UUID clinicalServiceId) {
        return mapper.toResponse(getClinicalServicesUseCase.getById(clinicalServiceId));
    }

    @PostMapping
    @RequirePermission(MANAGE_PERMISSION)
    public ResponseEntity<ClinicalServiceManagementResponse> create(
            @Valid @RequestBody CreateClinicalServiceRequest request
    ) {
        ClinicalServiceManagementResponse response = mapper.toResponse(
                createClinicalServiceUseCase.create(mapper.toCommand(request)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{clinicalServiceId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{clinicalServiceId}")
    @RequirePermission(MANAGE_PERMISSION)
    public ClinicalServiceManagementResponse update(
            @PathVariable UUID clinicalServiceId,
            @Valid @RequestBody UpdateClinicalServiceRequest request
    ) {
        return mapper.toResponse(updateClinicalServiceUseCase.update(clinicalServiceId, mapper.toCommand(request)));
    }

    @PatchMapping("/{clinicalServiceId}/status")
    @RequirePermission(MANAGE_PERMISSION)
    public ClinicalServiceManagementResponse updateStatus(
            @PathVariable UUID clinicalServiceId,
            @Valid @RequestBody UpdateClinicalServiceStatusRequest request
    ) {
        return mapper.toResponse(updateClinicalServiceStatusUseCase.updateStatus(clinicalServiceId, request.active()));
    }

    @GetMapping("/{clinicalServiceId}/reference-ranges")
    @RequirePermission(MANAGE_PERMISSION)
    public List<ClinicalReferenceRangeResponse> listRanges(@PathVariable UUID clinicalServiceId) {
        return getClinicalReferenceRangesUseCase.list(clinicalServiceId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/{clinicalServiceId}/reference-ranges")
    @RequirePermission(MANAGE_PERMISSION)
    public ResponseEntity<ClinicalReferenceRangeResponse> createRange(
            @PathVariable UUID clinicalServiceId,
            @Valid @RequestBody ClinicalReferenceRangeRequest request
    ) {
        ClinicalReferenceRangeResponse response = mapper.toResponse(
                createClinicalReferenceRangeUseCase.create(clinicalServiceId, mapper.toCreateCommand(request)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{referenceRangeId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{clinicalServiceId}/reference-ranges/{referenceRangeId}")
    @RequirePermission(MANAGE_PERMISSION)
    public ClinicalReferenceRangeResponse updateRange(
            @PathVariable UUID clinicalServiceId,
            @PathVariable UUID referenceRangeId,
            @Valid @RequestBody ClinicalReferenceRangeRequest request
    ) {
        return mapper.toResponse(updateClinicalReferenceRangeUseCase.update(
                clinicalServiceId, referenceRangeId, mapper.toUpdateCommand(request)));
    }

    @PatchMapping("/{clinicalServiceId}/reference-ranges/{referenceRangeId}/status")
    @RequirePermission(MANAGE_PERMISSION)
    public ClinicalReferenceRangeResponse updateRangeStatus(
            @PathVariable UUID clinicalServiceId,
            @PathVariable UUID referenceRangeId,
            @Valid @RequestBody UpdateClinicalServiceStatusRequest request
    ) {
        return mapper.toResponse(updateClinicalReferenceRangeStatusUseCase.updateStatus(
                clinicalServiceId, referenceRangeId, request.active()));
    }
}
