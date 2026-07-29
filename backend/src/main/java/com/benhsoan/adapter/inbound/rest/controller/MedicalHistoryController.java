package com.benhsoan.adapter.inbound.rest.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.MedicalHistoryRestMapper;
import com.benhsoan.adapter.inbound.rest.response.patient.MedicalHistoryItemResponse;
import com.benhsoan.port.inbound.patient.ViewPatientMedicalHistoryUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/medical-history")
@RequiredArgsConstructor
@Validated
public class MedicalHistoryController {

    private final ViewPatientMedicalHistoryUseCase viewPatientMedicalHistoryUseCase;
    private final MedicalHistoryRestMapper mapper;

    @GetMapping("/patients/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public Page<MedicalHistoryItemResponse> getPatientMedicalHistory(
            @PathVariable UUID patientId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return mapper.toResponse(viewPatientMedicalHistoryUseCase.viewMedicalHistory(
                mapper.toQuery(patientId, from, to, page, size)
        ));
    }
}
