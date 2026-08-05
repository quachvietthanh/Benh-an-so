package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.PrescriptionRestMapper;
import com.benhsoan.adapter.inbound.rest.request.prescription.CheckDrugInteractionRequest;
import com.benhsoan.adapter.inbound.rest.request.prescription.CreatePrescriptionRequest;
import com.benhsoan.adapter.inbound.rest.response.prescription.DrugInteractionWarningResponse;
import com.benhsoan.adapter.inbound.rest.response.prescription.PrescriptionResponse;
import com.benhsoan.port.dto.result.PrescriptionResult;
import com.benhsoan.port.inbound.prescription.CheckDrugInteractionUseCase;
import com.benhsoan.port.inbound.prescription.CreatePrescriptionUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/prescriptions")
@RequiredArgsConstructor
@Validated
public class PrescriptionController {

    private final CreatePrescriptionUseCase createPrescriptionUseCase;

    private final CheckDrugInteractionUseCase checkDrugInteractionUseCase;

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

    @PostMapping("/check-interactions")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public List<DrugInteractionWarningResponse> checkInteractions(
            @Valid @RequestBody CheckDrugInteractionRequest request
    ) {
        return mapper.toResponse(
                checkDrugInteractionUseCase.check(mapper.toCommand(request))
        );
    }
}
