package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordTemplateRestMapper;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.SpecialtyResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.inbound.specialty.SearchSpecialtyUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/system/specialties")
public class SpecialtyController {

    private static final String MANAGE_PERMISSION = "MEDICAL_RECORD_TEMPLATE_MANAGE";

    private final SearchSpecialtyUseCase searchSpecialtyUseCase;
    private final MedicalRecordTemplateRestMapper mapper;

    @GetMapping
    @RequirePermission(MANAGE_PERMISSION)
    public List<SpecialtyResponse> search(@RequestParam(required = false) Boolean active) {
        return searchSpecialtyUseCase.search(active).stream().map(mapper::toResponse).toList();
    }
}
