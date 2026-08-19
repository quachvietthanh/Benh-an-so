package com.benhsoan.adapter.inbound.rest.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.PatientRestMapper;
import com.benhsoan.adapter.inbound.rest.request.patient.RegisterPatientRequest;
import com.benhsoan.adapter.inbound.rest.request.patient.SearchPatientRequest;
import com.benhsoan.adapter.inbound.rest.request.patient.UpdatePatientRequest;
import com.benhsoan.adapter.inbound.rest.response.patient.PatientResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.result.PatientResult;
import com.benhsoan.port.inbound.patient.GetPatientByCodeUseCase;
import com.benhsoan.port.inbound.patient.GetPatientByIdUseCase;
import com.benhsoan.port.inbound.patient.RegisterPatientUseCase;
import com.benhsoan.port.inbound.patient.SearchPatientUseCase;
import com.benhsoan.port.inbound.patient.UpdatePatientUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/patients")
@RequiredArgsConstructor
@Validated
public class PatientController {

    private final RegisterPatientUseCase registerPatientUseCase;

    private final SearchPatientUseCase searchPatientUseCase;

    private final UpdatePatientUseCase updatePatientUseCase;

    private final GetPatientByIdUseCase getPatientByIdUseCase;

    private final GetPatientByCodeUseCase getPatientByCodeUseCase;

    private final PatientRestMapper patientRestMapper;

    @PostMapping
    @RequirePermission("PATIENT_CREATE")
    public PatientResponse register(
            @Valid @RequestBody RegisterPatientRequest request
    ) {

        PatientResult result =
                registerPatientUseCase.register(
                        patientRestMapper.toCommand(request));

        return patientRestMapper.toResponse(result);
    }

    @GetMapping
    @RequirePermission("PATIENT_READ")
    public Page<PatientResponse> search(
        SearchPatientRequest request,
        Pageable pageable ) {
                return patientRestMapper.toResponse( 
                        searchPatientUseCase.search(
                                patientRestMapper.toCommand(
                                        request,
                                        pageable
                                )
                        )
                );
        }

    @GetMapping("/code/{code}")
    @RequirePermission("PATIENT_READ")
    public PatientResponse getByCode(@PathVariable String code) {
        return patientRestMapper.toResponse(getPatientByCodeUseCase.getByCode(code));
    }

    @GetMapping("/{patientId}")
    @RequirePermission("PATIENT_READ")
    public PatientResponse getById(@PathVariable UUID patientId) {
        return patientRestMapper.toResponse(getPatientByIdUseCase.getById(patientId));
    }

    @PutMapping("/{patientId}")
    @RequirePermission("PATIENT_UPDATE")
    public PatientResponse update(

            @PathVariable
            UUID patientId,

            @Valid
            @RequestBody
            UpdatePatientRequest request

    ) {

        PatientResult result =
                updatePatientUseCase.update(
                        patientId,
                        patientRestMapper.toCommand(request));

        return patientRestMapper.toResponse(result);
    }

}
