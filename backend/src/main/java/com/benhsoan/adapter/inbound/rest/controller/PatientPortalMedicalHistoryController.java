package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.PatientPortalMedicalHistoryRestMapper;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientMedicalHistoryDetailResponse;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientMedicalHistorySummaryResponse;
import com.benhsoan.port.inbound.patient.GetPatientMedicalHistoryDetailUseCase;
import com.benhsoan.port.inbound.patient.GetPatientMedicalHistoryUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/patient-portal/medical-history")
@RequiredArgsConstructor
public class PatientPortalMedicalHistoryController {

    private final GetPatientMedicalHistoryUseCase getPatientMedicalHistoryUseCase;
    private final GetPatientMedicalHistoryDetailUseCase getPatientMedicalHistoryDetailUseCase;
    private final PatientPortalMedicalHistoryRestMapper mapper;

    @GetMapping
    public List<PatientMedicalHistorySummaryResponse> getMedicalHistory() {
        return getPatientMedicalHistoryUseCase.getMedicalHistory().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/{visitId}")
    public PatientMedicalHistoryDetailResponse getMedicalHistoryDetail(@PathVariable UUID visitId) {
        return mapper.toResponse(getPatientMedicalHistoryDetailUseCase.getMedicalHistoryDetail(visitId));
    }
}
