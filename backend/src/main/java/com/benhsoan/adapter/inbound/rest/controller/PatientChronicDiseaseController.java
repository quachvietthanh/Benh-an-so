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

import com.benhsoan.adapter.inbound.rest.mapper.PatientChronicDiseaseRestMapper;
import com.benhsoan.adapter.inbound.rest.request.patient.AddPatientChronicDiseaseRequest;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientChronicDiseaseResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.command.patient.DeletePatientChronicDiseaseCommand;
import com.benhsoan.port.inbound.patient.AddPatientChronicDiseaseUseCase;
import com.benhsoan.port.inbound.patient.DeletePatientChronicDiseaseUseCase;
import com.benhsoan.port.inbound.patient.GetPatientChronicDiseasesUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/patients/{patientId}/chronic-diseases")
@RequiredArgsConstructor
@Validated
public class PatientChronicDiseaseController {

    private final AddPatientChronicDiseaseUseCase addPatientChronicDiseaseUseCase;
    private final GetPatientChronicDiseasesUseCase getPatientChronicDiseasesUseCase;
    private final DeletePatientChronicDiseaseUseCase deletePatientChronicDiseaseUseCase;
    private final PatientChronicDiseaseRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("PATIENT_CHRONIC_DISEASE_WRITE")
    public PatientChronicDiseaseResponse addChronicDisease(
            @PathVariable UUID patientId,
            @Valid @RequestBody AddPatientChronicDiseaseRequest request
    ) {
        return mapper.toResponse(
                addPatientChronicDiseaseUseCase.addChronicDisease(mapper.toCommand(patientId, request))
        );
    }

    @GetMapping
    @RequirePermission("PATIENT_CHRONIC_DISEASE_READ")
    public List<PatientChronicDiseaseResponse> getChronicDiseases(@PathVariable UUID patientId) {
        return getPatientChronicDiseasesUseCase.getChronicDiseases(patientId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @DeleteMapping("/{chronicDiseaseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission("PATIENT_CHRONIC_DISEASE_WRITE")
    public void deleteChronicDisease(
            @PathVariable UUID patientId,
            @PathVariable UUID chronicDiseaseId,
            @RequestParam(required = false) String reason
    ) {
        deletePatientChronicDiseaseUseCase.deleteChronicDisease(
                DeletePatientChronicDiseaseCommand.builder()
                        .chronicDiseaseId(chronicDiseaseId)
                        .patientId(patientId)
                        .reason(reason)
                        .build()
        );
    }
}
