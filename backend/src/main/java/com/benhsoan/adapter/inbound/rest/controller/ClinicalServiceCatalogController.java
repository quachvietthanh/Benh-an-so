package com.benhsoan.adapter.inbound.rest.controller;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.ClinicalServiceCatalogRestMapper;
import com.benhsoan.adapter.inbound.rest.response.clinical.ClinicalServiceCatalogResponse;
import com.benhsoan.port.dto.command.clinical.SearchClinicalServiceCatalogQuery;
import com.benhsoan.port.inbound.clinical.SearchClinicalServiceCatalogUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/clinical-services")
@RequiredArgsConstructor
@Validated
public class ClinicalServiceCatalogController {

    private final SearchClinicalServiceCatalogUseCase searchClinicalServiceCatalogUseCase;
    private final ClinicalServiceCatalogRestMapper mapper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public Page<ClinicalServiceCatalogResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return searchClinicalServiceCatalogUseCase.search(new SearchClinicalServiceCatalogQuery(keyword, page, size))
                .map(mapper::toResponse);
    }
}
