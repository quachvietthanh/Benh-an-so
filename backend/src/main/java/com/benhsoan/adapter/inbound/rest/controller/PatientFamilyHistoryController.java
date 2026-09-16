package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.PatientFamilyHistoryRestMapper;
import com.benhsoan.adapter.inbound.rest.request.patient.AddPatientFamilyHistoryRequest;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientFamilyHistoryResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.command.patient.DeletePatientFamilyHistoryCommand;
import com.benhsoan.port.inbound.patient.AddPatientFamilyHistoryUseCase;
import com.benhsoan.port.inbound.patient.DeletePatientFamilyHistoryUseCase;
import com.benhsoan.port.inbound.patient.GetPatientFamilyHistoryUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/patients/{patientId}/family-history")
@RequiredArgsConstructor
@Validated
public class PatientFamilyHistoryController {

    private final AddPatientFamilyHistoryUseCase addPatientFamilyHistoryUseCase;
    private final GetPatientFamilyHistoryUseCase getPatientFamilyHistoryUseCase;
    private final DeletePatientFamilyHistoryUseCase deletePatientFamilyHistoryUseCase;
    private final PatientFamilyHistoryRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("PATIENT_FAMILY_HISTORY_WRITE")
    public PatientFamilyHistoryResponse addFamilyHistory(
            @PathVariable UUID patientId,
            @Valid @RequestBody AddPatientFamilyHistoryRequest request
    ) {
        return mapper.toResponse(
                addPatientFamilyHistoryUseCase.addFamilyHistory(mapper.toCommand(patientId, request))
        );
    }

    @GetMapping
    @RequirePermission("PATIENT_FAMILY_HISTORY_READ")
    public List<PatientFamilyHistoryResponse> getFamilyHistory(@PathVariable UUID patientId) {
        return getPatientFamilyHistoryUseCase.getFamilyHistory(patientId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @DeleteMapping("/{familyHistoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission("PATIENT_FAMILY_HISTORY_WRITE")
    public void deleteFamilyHistory(
            @PathVariable UUID patientId,
            @PathVariable UUID familyHistoryId,
            @RequestParam(required = false) String reason
    ) {
        deletePatientFamilyHistoryUseCase.deleteFamilyHistory(
                DeletePatientFamilyHistoryCommand.builder()
                        .familyHistoryId(familyHistoryId)
                        .patientId(patientId)
                        .reason(reason)
                        .build()
        );
    }
}
