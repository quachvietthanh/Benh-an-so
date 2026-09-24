package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.PatientConsentRestMapper;
import com.benhsoan.adapter.inbound.rest.request.patient.RequestPatientDataErasureRequest;
import com.benhsoan.adapter.inbound.rest.response.patient.DataErasureResponse;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientConsentHistoryResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.inbound.patient.GetPatientConsentHistoryUseCase;
import com.benhsoan.port.inbound.patient.RequestPatientDataErasureUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller quản lý lịch sử phiếu đồng ý và tiếp nhận yêu cầu xóa dữ liệu theo QTN-19
 * (User Story NCL-15-CN-005 / AC-02, AC-03).
 */
@RestController
@RequestMapping("/patients/{patientId}")
@RequiredArgsConstructor
@Validated
public class PatientConsentManagementController {

    private final GetPatientConsentHistoryUseCase getPatientConsentHistoryUseCase;
    private final RequestPatientDataErasureUseCase requestPatientDataErasureUseCase;
    private final PatientConsentRestMapper mapper;

    @GetMapping("/consent-history")
    @RequirePermission({"PATIENT_READ", "PATIENT_CONSENT_UPDATE"})
    public List<PatientConsentHistoryResponse> getConsentHistory(@PathVariable UUID patientId) {
        return getPatientConsentHistoryUseCase.getConsentHistory(patientId).stream()
                .map(mapper::toHistoryResponse)
                .toList();
    }

    @PostMapping("/data-erasure-request")
    @RequirePermission("PATIENT_CONSENT_UPDATE")
    public DataErasureResponse requestDataErasure(
            @PathVariable UUID patientId,
            @Valid @RequestBody(required = false) RequestPatientDataErasureRequest request
    ) {
        return mapper.toErasureResponse(
                requestPatientDataErasureUseCase.requestErasure(
                        patientId,
                        mapper.toErasureCommand(request)
                )
        );
    }
}
