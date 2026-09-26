package com.benhsoan.adapter.inbound.rest.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.AnonymizationModeRestMapper;
import com.benhsoan.adapter.inbound.rest.request.anonymization.UpdateAnonymizationModeRequest;
import com.benhsoan.adapter.inbound.rest.response.anonymization.AnonymizationModeResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.inbound.anonymization.GetAnonymizationModeUseCase;
import com.benhsoan.port.inbound.anonymization.UpdateAnonymizationModeUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/anonymization")
public class AnonymizationModeController {

    private final GetAnonymizationModeUseCase getAnonymizationModeUseCase;
    private final UpdateAnonymizationModeUseCase updateAnonymizationModeUseCase;
    private final AnonymizationModeRestMapper mapper;

    @GetMapping
    @RequirePermission("SYSTEM_CONFIG_READ")
    public AnonymizationModeResponse get() {
        return mapper.toResponse(getAnonymizationModeUseCase.get());
    }

    @PatchMapping
    @RequirePermission("SYSTEM_CONFIG_UPDATE")
    public AnonymizationModeResponse update(@Valid @RequestBody UpdateAnonymizationModeRequest request) {
        return mapper.toResponse(updateAnonymizationModeUseCase.update(mapper.toCommand(request)));
    }
}
