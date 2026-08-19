package com.benhsoan.adapter.inbound.rest.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.ClinicConfigurationRestMapper;
import com.benhsoan.adapter.inbound.rest.request.clinic.UpdateClinicConfigurationRequest;
import com.benhsoan.adapter.inbound.rest.response.clinic.ClinicConfigurationResponse;
import com.benhsoan.port.inbound.clinic.GetClinicConfigurationUseCase;
import com.benhsoan.port.inbound.clinic.UpdateClinicConfigurationUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/clinic")
public class ClinicConfigurationController {

    private final GetClinicConfigurationUseCase getClinicConfigurationUseCase;
    private final UpdateClinicConfigurationUseCase updateClinicConfigurationUseCase;
    private final ClinicConfigurationRestMapper mapper;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ClinicConfigurationResponse get() {
        return mapper.toResponse(getClinicConfigurationUseCase.get());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ClinicConfigurationResponse update(@Valid @RequestBody UpdateClinicConfigurationRequest request) {
        return mapper.toResponse(updateClinicConfigurationUseCase.update(mapper.toCommand(request)));
    }
}
