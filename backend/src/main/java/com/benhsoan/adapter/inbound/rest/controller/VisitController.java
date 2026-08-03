package com.benhsoan.adapter.inbound.rest.controller;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.VisitRestMapper;
import com.benhsoan.adapter.inbound.rest.response.visit.VisitEncounterResponse;
import com.benhsoan.port.inbound.visit.GetVisitEncounterUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/visits")
@RequiredArgsConstructor
public class VisitController {

    private final GetVisitEncounterUseCase getVisitEncounterUseCase;
    private final VisitRestMapper mapper;

    @GetMapping("/{visitId}/encounter")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public VisitEncounterResponse getEncounter(@PathVariable UUID visitId) {
        return mapper.toResponse(getVisitEncounterUseCase.getEncounter(visitId));
    }
}
