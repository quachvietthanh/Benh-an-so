package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.DiagnosisCatalogRestMapper;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.DiagnosisCatalogResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.inbound.medicalrecord.GetDiagnosisCatalogUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/diagnosis-catalog")
@RequiredArgsConstructor
@Validated
public class DiagnosisCatalogController {

    private final GetDiagnosisCatalogUseCase getDiagnosisCatalogUseCase;
    private final DiagnosisCatalogRestMapper mapper;

    @GetMapping
    @RequirePermission("DIAGNOSIS_READ")
    public List<DiagnosisCatalogResponse> search(
            @RequestParam(required = false) String search) {
        return mapper.toResponse(getDiagnosisCatalogUseCase.search(search));
    }
}
