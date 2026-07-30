package com.benhsoan.adapter.inbound.rest.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.ClinicalOrderRestMapper;
import com.benhsoan.adapter.inbound.rest.request.clinical.CreateClinicalOrderRequest;
import com.benhsoan.adapter.inbound.rest.response.clinical.ClinicalOrderResponse;
import com.benhsoan.port.inbound.clinical.CreateClinicalOrderUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/clinical-orders")
@RequiredArgsConstructor
@Validated
public class ClinicalOrderController {

    private final CreateClinicalOrderUseCase createClinicalOrderUseCase;
    private final ClinicalOrderRestMapper mapper;

    @PostMapping("/visits/{visitId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ClinicalOrderResponse create(
            @PathVariable UUID visitId,
            @Valid @RequestBody CreateClinicalOrderRequest request
    ) {
        return mapper.toResponse(createClinicalOrderUseCase.createOrder(visitId, mapper.toCommand(request)));
    }
}
