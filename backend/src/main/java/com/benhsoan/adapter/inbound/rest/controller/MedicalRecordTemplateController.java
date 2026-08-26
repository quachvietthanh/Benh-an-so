package com.benhsoan.adapter.inbound.rest.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.benhsoan.adapter.inbound.rest.mapper.MedicalRecordTemplateRestMapper;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.CreateMedicalRecordTemplateRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.UpdateMedicalRecordTemplateRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.UpdateMedicalRecordTemplateStatusRequest;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordTemplateResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordTemplateSummaryResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.command.medicalrecord.SearchMedicalRecordTemplateQuery;
import com.benhsoan.port.inbound.medicalrecord.CreateMedicalRecordTemplateUseCase;
import com.benhsoan.port.inbound.medicalrecord.MedicalRecordTemplateQueryUseCase;
import com.benhsoan.port.inbound.medicalrecord.SetMedicalRecordTemplateDefaultUseCase;
import com.benhsoan.port.inbound.medicalrecord.UpdateMedicalRecordTemplateStatusUseCase;
import com.benhsoan.port.inbound.medicalrecord.UpdateMedicalRecordTemplateUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/system/medical-record-templates")
public class MedicalRecordTemplateController {

    private static final String MANAGE_PERMISSION = "MEDICAL_RECORD_TEMPLATE_MANAGE";

    private final MedicalRecordTemplateQueryUseCase queryUseCase;
    private final CreateMedicalRecordTemplateUseCase createUseCase;
    private final UpdateMedicalRecordTemplateUseCase updateUseCase;
    private final SetMedicalRecordTemplateDefaultUseCase setDefaultUseCase;
    private final UpdateMedicalRecordTemplateStatusUseCase updateStatusUseCase;
    private final MedicalRecordTemplateRestMapper mapper;

    @GetMapping
    @RequirePermission(MANAGE_PERMISSION)
    public List<MedicalRecordTemplateSummaryResponse> search(@RequestParam(required = false) UUID specialtyId,
            @RequestParam(required = false) Boolean active) {
        return queryUseCase.search(new SearchMedicalRecordTemplateQuery(specialtyId, active)).stream()
                .map(mapper::toSummaryResponse).toList();
    }

    @GetMapping("/{templateId}")
    @RequirePermission(MANAGE_PERMISSION)
    public MedicalRecordTemplateResponse getById(@PathVariable UUID templateId) {
        return mapper.toResponse(queryUseCase.getById(templateId));
    }

    @PostMapping
    @RequirePermission(MANAGE_PERMISSION)
    public ResponseEntity<MedicalRecordTemplateResponse> create(
            @Valid @RequestBody CreateMedicalRecordTemplateRequest request) {
        MedicalRecordTemplateResponse response = mapper.toResponse(createUseCase.create(mapper.toCommand(request)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{templateId}")
                .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{templateId}")
    @RequirePermission(MANAGE_PERMISSION)
    public MedicalRecordTemplateResponse update(@PathVariable UUID templateId,
            @Valid @RequestBody UpdateMedicalRecordTemplateRequest request) {
        return mapper.toResponse(updateUseCase.update(mapper.toCommand(templateId, request)));
    }

    @PatchMapping("/{templateId}/default")
    @RequirePermission(MANAGE_PERMISSION)
    public MedicalRecordTemplateResponse setDefault(@PathVariable UUID templateId) {
        return mapper.toResponse(setDefaultUseCase.setDefault(templateId));
    }

    @PatchMapping("/{templateId}/status")
    @RequirePermission(MANAGE_PERMISSION)
    public MedicalRecordTemplateResponse updateStatus(@PathVariable UUID templateId,
            @Valid @RequestBody UpdateMedicalRecordTemplateStatusRequest request) {
        return mapper.toResponse(updateStatusUseCase.updateStatus(mapper.toCommand(templateId, request)));
    }
}
