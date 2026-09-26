package com.benhsoan.adapter.inbound.rest.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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

import com.benhsoan.adapter.inbound.rest.mapper.DiagnosisCatalogRestMapper;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.CreateDiagnosisCatalogRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.UpdateDiagnosisCatalogRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.UpdateDiagnosisCatalogStatusRequest;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.DiagnosisCatalogResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.inbound.medicalrecord.CreateDiagnosisCatalogUseCase;
import com.benhsoan.port.inbound.medicalrecord.DeleteDiagnosisCatalogUseCase;
import com.benhsoan.port.inbound.medicalrecord.DiagnosisCatalogManagementQueryUseCase;
import com.benhsoan.port.inbound.medicalrecord.UpdateDiagnosisCatalogStatusUseCase;
import com.benhsoan.port.inbound.medicalrecord.UpdateDiagnosisCatalogUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/diagnosis-catalog")
public class DiagnosisCatalogManagementController {

    private static final String MANAGE_PERMISSION = "DIAGNOSIS_CATALOG_MANAGE";

    private final DiagnosisCatalogManagementQueryUseCase queryUseCase;
    private final CreateDiagnosisCatalogUseCase createUseCase;
    private final UpdateDiagnosisCatalogUseCase updateUseCase;
    private final UpdateDiagnosisCatalogStatusUseCase updateStatusUseCase;
    private final DeleteDiagnosisCatalogUseCase deleteUseCase;
    private final DiagnosisCatalogRestMapper mapper;

    @GetMapping
    @RequirePermission(MANAGE_PERMISSION)
    public List<DiagnosisCatalogResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active
    ) {
        return mapper.toResponse(queryUseCase.search(keyword, active));
    }

    @GetMapping("/{diagnosisCatalogId}")
    @RequirePermission(MANAGE_PERMISSION)
    public DiagnosisCatalogResponse getById(@PathVariable UUID diagnosisCatalogId) {
        return mapper.toResponse(queryUseCase.getById(diagnosisCatalogId));
    }

    @PostMapping
    @RequirePermission(MANAGE_PERMISSION)
    public ResponseEntity<DiagnosisCatalogResponse> create(
            @Valid @RequestBody CreateDiagnosisCatalogRequest request
    ) {
        DiagnosisCatalogResponse response = mapper.toResponse(createUseCase.create(mapper.toCommand(request)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{diagnosisCatalogId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{diagnosisCatalogId}")
    @RequirePermission(MANAGE_PERMISSION)
    public DiagnosisCatalogResponse update(
            @PathVariable UUID diagnosisCatalogId,
            @Valid @RequestBody UpdateDiagnosisCatalogRequest request
    ) {
        return mapper.toResponse(updateUseCase.update(mapper.toCommand(diagnosisCatalogId, request)));
    }

    @PatchMapping("/{diagnosisCatalogId}/status")
    @RequirePermission(MANAGE_PERMISSION)
    public DiagnosisCatalogResponse updateStatus(
            @PathVariable UUID diagnosisCatalogId,
            @Valid @RequestBody UpdateDiagnosisCatalogStatusRequest request
    ) {
        return mapper.toResponse(updateStatusUseCase.updateStatus(diagnosisCatalogId, request.active()));
    }

    @DeleteMapping("/{diagnosisCatalogId}")
    @RequirePermission(MANAGE_PERMISSION)
    public void delete(@PathVariable UUID diagnosisCatalogId) {
        deleteUseCase.delete(diagnosisCatalogId);
    }
}
