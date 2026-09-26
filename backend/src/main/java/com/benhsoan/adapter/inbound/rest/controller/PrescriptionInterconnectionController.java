package com.benhsoan.adapter.inbound.rest.controller;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.domain.prescription.enums.InterconnectionStatus;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.command.prescription.SearchPrescriptionInterconnectionsQuery;
import com.benhsoan.port.dto.result.PrescriptionInterconnectionListItemResult;
import com.benhsoan.port.inbound.prescription.SearchPrescriptionInterconnectionsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/prescription-interconnections")
@RequiredArgsConstructor
public class PrescriptionInterconnectionController {

    private final SearchPrescriptionInterconnectionsUseCase searchPrescriptionInterconnectionsUseCase;

    @GetMapping
    @RequirePermission("PRESCRIPTION_INTERCONNECTION_READ")
    @Operation(summary = "Search prescription interconnection submissions")
    @ApiResponse(responseCode = "200", description = "Paginated interconnection submissions")
    @ApiResponse(responseCode = "403", description = "Requires interconnection read permission")
    public Page<PrescriptionInterconnectionListItemResult> search(
            @RequestParam InterconnectionStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return searchPrescriptionInterconnectionsUseCase.search(
                new SearchPrescriptionInterconnectionsQuery(status, from, to, page, size));
    }
}
