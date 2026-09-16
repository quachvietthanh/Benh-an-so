package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.VitalSignRestMapper;
import com.benhsoan.adapter.inbound.rest.request.vitalsign.RecordVitalSignRequest;
import com.benhsoan.adapter.inbound.rest.request.vitalsign.UpdateVitalSignRequest;
import com.benhsoan.adapter.inbound.rest.response.vitalsign.VitalSignResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.inbound.vitalsign.GetPatientVitalSignHistoryUseCase;
import com.benhsoan.port.inbound.vitalsign.GetVitalSignUseCase;
import com.benhsoan.port.inbound.vitalsign.RecordVitalSignUseCase;
import com.benhsoan.port.inbound.vitalsign.UpdateVitalSignUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/vital-signs")
@RequiredArgsConstructor
@Validated
public class VitalSignController {

    private final RecordVitalSignUseCase recordVitalSignUseCase;
    private final UpdateVitalSignUseCase updateVitalSignUseCase;
    private final GetVitalSignUseCase getVitalSignUseCase;
    private final GetPatientVitalSignHistoryUseCase getPatientVitalSignHistoryUseCase;
    private final VitalSignRestMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("VITAL_SIGN_CREATE")
    public VitalSignResponse record(@Valid @RequestBody RecordVitalSignRequest request) {
        return mapper.toResponse(recordVitalSignUseCase.record(mapper.toCommand(request)));
    }

    @PutMapping("/{id}")
    @RequirePermission("VITAL_SIGN_UPDATE")
    public VitalSignResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateVitalSignRequest request) {
        return mapper.toResponse(updateVitalSignUseCase.update(id, mapper.toCommand(request)));
    }

    @GetMapping("/{id}")
    @RequirePermission("VITAL_SIGN_READ")
    public VitalSignResponse getById(@PathVariable UUID id) {
        return mapper.toResponse(getVitalSignUseCase.getById(id));
    }

    @GetMapping("/visits/{visitId}")
    @RequirePermission("VITAL_SIGN_READ")
    public List<VitalSignResponse> getByVisitId(@PathVariable UUID visitId) {
        return mapper.toResponses(getVitalSignUseCase.getByVisitId(visitId));
    }

    @GetMapping("/patients/{patientId}/history")
    @RequirePermission("VITAL_SIGN_READ")
    public List<VitalSignResponse> getPatientHistory(@PathVariable UUID patientId) {
        return mapper.toResponses(getPatientVitalSignHistoryUseCase.getHistory(patientId));
    }

    @GetMapping
    @RequirePermission("VITAL_SIGN_READ")
    public List<VitalSignResponse> search(
            @RequestParam(required = false) UUID visitId,
            @RequestParam(required = false) UUID patientId
    ) {
        if (patientId != null) {
            return mapper.toResponses(getPatientVitalSignHistoryUseCase.getHistory(patientId));
        }
        if (visitId != null) {
            return mapper.toResponses(getVitalSignUseCase.getByVisitId(visitId));
        }
        return List.of();
    }
}
