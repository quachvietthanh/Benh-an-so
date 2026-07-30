package com.benhsoan.adapter.inbound.rest.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.ExaminationDiagnosisRestMapper;
import com.benhsoan.adapter.inbound.rest.request.clinical.CreateClinicalOrderRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.RecordDiagnosisRequest;
import com.benhsoan.adapter.inbound.rest.response.clinical.ClinicalOrderResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.ExaminationDiagnosisResponse;
import com.benhsoan.port.inbound.clinical.CreateClinicalOrderUseCase;
import com.benhsoan.port.inbound.medicalrecord.GetExaminationDiagnosisUseCase;
import com.benhsoan.port.inbound.medicalrecord.RecordDiagnosisUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/examinations/{examinationId}")
@RequiredArgsConstructor
@Validated
public class ExaminationDiagnosisController {

    private final RecordDiagnosisUseCase recordDiagnosisUseCase;
    private final GetExaminationDiagnosisUseCase getExaminationDiagnosisUseCase;
    private final CreateClinicalOrderUseCase createClinicalOrderUseCase;
    private final ExaminationDiagnosisRestMapper mapper;

    @PostMapping("/diagnosis")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ExaminationDiagnosisResponse recordDiagnosis(
            @PathVariable UUID examinationId,
            @Valid @RequestBody RecordDiagnosisRequest request) {
        var result = recordDiagnosisUseCase.recordDiagnosis(
                examinationId, mapper.toCommand(request));
        return mapper.toResponse(result);
    }

    @GetMapping("/diagnosis")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public ExaminationDiagnosisResponse getDiagnosis(
            @PathVariable UUID examinationId) {
        var result = getExaminationDiagnosisUseCase.getDiagnosis(examinationId);
        return mapper.toResponse(result);
    }

    @PostMapping("/clinical-orders")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ClinicalOrderResponse createClinicalOrder(
            @PathVariable UUID examinationId,
            @Valid @RequestBody CreateClinicalOrderRequest request) {
        var result = createClinicalOrderUseCase.createOrder(
                examinationId, mapper.toCommand(request));
        return mapper.toResponse(result);
    }
}
