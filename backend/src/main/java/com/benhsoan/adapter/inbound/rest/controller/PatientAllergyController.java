package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.PatientAllergyRestMapper;
import com.benhsoan.adapter.inbound.rest.request.patient.AddPatientAllergyRequest;
import com.benhsoan.adapter.inbound.rest.request.patient.UpdatePatientAllergyRequest;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientAllergyChangeLogResponse;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientAllergyResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.command.patient.DeletePatientAllergyCommand;
import com.benhsoan.port.inbound.patient.AddPatientAllergyUseCase;
import com.benhsoan.port.inbound.patient.DeletePatientAllergyUseCase;
import com.benhsoan.port.inbound.patient.GetPatientAllergiesUseCase;
import com.benhsoan.port.inbound.patient.GetPatientAllergyChangeLogsUseCase;
import com.benhsoan.port.inbound.patient.UpdatePatientAllergyUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/patients/{patientId}/allergies")
@RequiredArgsConstructor
@Validated
public class PatientAllergyController {

    private final AddPatientAllergyUseCase addPatientAllergyUseCase;
    private final UpdatePatientAllergyUseCase updatePatientAllergyUseCase;
    private final DeletePatientAllergyUseCase deletePatientAllergyUseCase;
    private final GetPatientAllergiesUseCase getPatientAllergiesUseCase;
    private final GetPatientAllergyChangeLogsUseCase getPatientAllergyChangeLogsUseCase;
    private final PatientAllergyRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("PATIENT_ALLERGY_WRITE")
    public PatientAllergyResponse addAllergy(
            @PathVariable UUID patientId,
            @Valid @RequestBody AddPatientAllergyRequest request
    ) {
        return mapper.toResponse(
                addPatientAllergyUseCase.addAllergy(mapper.toCommand(patientId, request))
        );
    }

    @GetMapping
    @RequirePermission("PATIENT_ALLERGY_READ")
    public List<PatientAllergyResponse> getAllergies(@PathVariable UUID patientId) {
        return getPatientAllergiesUseCase.getAllergies(patientId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @PutMapping("/{allergyId}")
    @RequirePermission("PATIENT_ALLERGY_WRITE")
    public PatientAllergyResponse updateAllergy(
            @PathVariable UUID patientId,
            @PathVariable UUID allergyId,
            @Valid @RequestBody UpdatePatientAllergyRequest request
    ) {
        return mapper.toResponse(
                updatePatientAllergyUseCase.updateAllergy(mapper.toCommand(patientId, allergyId, request))
        );
    }

    @DeleteMapping("/{allergyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission("PATIENT_ALLERGY_WRITE")
    public void deleteAllergy(
            @PathVariable UUID patientId,
            @PathVariable UUID allergyId,
            @RequestParam(required = false) String reason
    ) {
        deletePatientAllergyUseCase.deleteAllergy(
                DeletePatientAllergyCommand.builder()
                        .allergyId(allergyId)
                        .patientId(patientId)
                        .reason(reason)
                        .build()
        );
    }

    @GetMapping("/{allergyId}/history")
    @RequirePermission("PATIENT_ALLERGY_WRITE")
    public List<PatientAllergyChangeLogResponse> getChangeLogs(
            @PathVariable UUID patientId,
            @PathVariable UUID allergyId
    ) {
        return getPatientAllergyChangeLogsUseCase.getChangeLogs(patientId, allergyId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}
