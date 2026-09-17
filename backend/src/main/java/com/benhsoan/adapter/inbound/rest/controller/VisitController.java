package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.VisitRestMapper;
import com.benhsoan.adapter.inbound.rest.request.visit.HandoverPatientRequest;
import com.benhsoan.adapter.inbound.rest.response.visit.VisitEncounterResponse;
import com.benhsoan.adapter.inbound.rest.response.visit.VisitHandoverResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.command.visit.HandoverPatientCommand;
import com.benhsoan.adapter.inbound.rest.response.visit.HandoverDoctorResponse;
import com.benhsoan.port.inbound.user.GetDoctorsUseCase;
import com.benhsoan.port.inbound.visit.GetVisitEncounterUseCase;
import com.benhsoan.port.inbound.visit.GetVisitHandoversUseCase;
import com.benhsoan.port.inbound.visit.HandoverPatientUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/visits")
@RequiredArgsConstructor
public class VisitController {

    private final GetVisitEncounterUseCase getVisitEncounterUseCase;
    private final HandoverPatientUseCase handoverPatientUseCase;
    private final GetVisitHandoversUseCase getVisitHandoversUseCase;
    private final GetDoctorsUseCase getDoctorsUseCase;
    private final VisitRestMapper mapper;

    @GetMapping("/{visitId}/encounter")
    @RequirePermission("MEDICAL_RECORD_READ")
    public VisitEncounterResponse getEncounter(@PathVariable UUID visitId) {
        return mapper.toResponse(getVisitEncounterUseCase.getEncounter(visitId));
    }

    @PostMapping("/{visitId}/handover")
    @ResponseStatus(HttpStatus.OK)
    @RequirePermission("MEDICAL_RECORD_HANDOVER")
    public VisitHandoverResponse handover(
            @PathVariable UUID visitId,
            @Valid @RequestBody HandoverPatientRequest request) {
        var command = new HandoverPatientCommand(request.targetDoctorId(), request.reason());
        return mapper.toResponse(handoverPatientUseCase.handover(visitId, command));
    }

    @GetMapping("/{visitId}/handovers")
    @RequirePermission("MEDICAL_RECORD_READ")
    public List<VisitHandoverResponse> getHandovers(@PathVariable UUID visitId) {
        return getVisitHandoversUseCase.getHandovers(visitId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/handover/doctors")
    @RequirePermission("MEDICAL_RECORD_READ")
    public List<HandoverDoctorResponse> getHandoverDoctors() {
        return mapper.toHandoverDoctorResponseList(getDoctorsUseCase.getAllActiveDoctors());
    }
}
