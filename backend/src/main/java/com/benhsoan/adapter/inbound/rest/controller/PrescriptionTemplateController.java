package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.PrescriptionTemplateRestMapper;
import com.benhsoan.adapter.inbound.rest.request.prescription.ApplyPrescriptionTemplateRequest;
import com.benhsoan.adapter.inbound.rest.request.prescription.SavePrescriptionTemplateRequest;
import com.benhsoan.adapter.inbound.rest.response.prescription.AppliedPrescriptionTemplateResponse;
import com.benhsoan.adapter.inbound.rest.response.prescription.PrescriptionTemplateResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.command.prescription.ApplyPrescriptionTemplateCommand;
import com.benhsoan.port.dto.command.prescription.SavePrescriptionTemplateCommand;
import com.benhsoan.port.inbound.prescription.ApplyPrescriptionTemplateUseCase;
import com.benhsoan.port.inbound.prescription.GetPrescriptionTemplateUseCase;
import com.benhsoan.port.inbound.prescription.GetPrescriptionTemplatesUseCase;
import com.benhsoan.port.inbound.prescription.SavePrescriptionTemplateUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * NCL-05-CN-008: prescription templates by diagnosis. Write endpoints require the
 * PRESCRIPTION_CREATE permission; read endpoints require PRESCRIPTION_READ. The
 * doctor role and ownership checks are enforced in the service layer.
 */
@RestController
@RequestMapping("/prescription-templates")
@RequiredArgsConstructor
@Validated
public class PrescriptionTemplateController {

    private final SavePrescriptionTemplateUseCase savePrescriptionTemplateUseCase;
    private final ApplyPrescriptionTemplateUseCase applyPrescriptionTemplateUseCase;
    private final GetPrescriptionTemplatesUseCase getPrescriptionTemplatesUseCase;
    private final GetPrescriptionTemplateUseCase getPrescriptionTemplateUseCase;
    private final PrescriptionTemplateRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("PRESCRIPTION_CREATE")
    public PrescriptionTemplateResponse save(@Valid @RequestBody SavePrescriptionTemplateRequest request) {
        return mapper.toResponse(savePrescriptionTemplateUseCase.save(
                new SavePrescriptionTemplateCommand(request.prescriptionId(), request.diagnosisCode())));
    }

    @GetMapping
    @RequirePermission("PRESCRIPTION_READ")
    public List<PrescriptionTemplateResponse> getByDiagnosisCode(
            @RequestParam String diagnosisCode
    ) {
        return getPrescriptionTemplatesUseCase.getByDiagnosisCode(diagnosisCode).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @RequirePermission("PRESCRIPTION_READ")
    public PrescriptionTemplateResponse getById(@PathVariable UUID id) {
        return mapper.toResponse(getPrescriptionTemplateUseCase.getById(id));
    }

    @PostMapping("/{id}/apply")
    @RequirePermission("PRESCRIPTION_CREATE")
    public AppliedPrescriptionTemplateResponse apply(
            @PathVariable UUID id,
            @Valid @RequestBody ApplyPrescriptionTemplateRequest request
    ) {
        return mapper.toResponse(applyPrescriptionTemplateUseCase.apply(
                new ApplyPrescriptionTemplateCommand(id, request.medicalRecordId())));
    }
}
