package com.benhsoan.adapter.inbound.rest.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.PrescriptionRestMapper;
import com.benhsoan.adapter.inbound.rest.request.prescription.AmendPrescriptionRequest;
import com.benhsoan.adapter.inbound.rest.request.prescription.CreatePrescriptionRequest;
import com.benhsoan.adapter.inbound.rest.response.prescription.PrescriptionResponse;
import com.benhsoan.port.dto.result.PrescriptionResult;
import com.benhsoan.port.inbound.prescription.AmendPrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.CreatePrescriptionUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/prescriptions")
@RequiredArgsConstructor
@Validated
public class PrescriptionController {

    private final CreatePrescriptionUseCase createPrescriptionUseCase;

    private final AmendPrescriptionUseCase amendPrescriptionUseCase;

    private final PrescriptionRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DOCTOR')")
    public PrescriptionResponse create(
            @Valid @RequestBody CreatePrescriptionRequest request
    ) {
        PrescriptionResult result = createPrescriptionUseCase.create(
                mapper.toCommand(request)
        );

        return mapper.toResponse(result);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public PrescriptionResponse amend(
            @PathVariable UUID id,
            @Valid @RequestBody AmendPrescriptionRequest request
    ) {
        PrescriptionResult result = amendPrescriptionUseCase.amend(
                mapper.toCommand(id, request)
        );

        return mapper.toResponse(result);
    }
}
