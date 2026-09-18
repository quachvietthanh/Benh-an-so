package com.benhsoan.adapter.inbound.rest.controller;

import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.VisitRestMapper;
import com.benhsoan.adapter.inbound.rest.response.visit.VisitEncounterResponse;
import com.benhsoan.adapter.inbound.rest.response.visit.VisitSummaryResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.inbound.visit.ExportVisitSummaryUseCase;
import com.benhsoan.port.inbound.visit.GetVisitEncounterUseCase;
import com.benhsoan.port.inbound.visit.GetVisitSummaryUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/visits")
@RequiredArgsConstructor
public class VisitController {

    private final GetVisitEncounterUseCase getVisitEncounterUseCase;
    private final GetVisitSummaryUseCase getVisitSummaryUseCase;
    private final ExportVisitSummaryUseCase exportVisitSummaryUseCase;
    private final VisitRestMapper mapper;

    @GetMapping("/{visitId}/encounter")
    @RequirePermission("MEDICAL_RECORD_READ")
    public VisitEncounterResponse getEncounter(@PathVariable UUID visitId) {
        return mapper.toResponse(getVisitEncounterUseCase.getEncounter(visitId));
    }

    @GetMapping("/{visitId}/summary")
    @RequirePermission("VISIT_SUMMARY_PRINT")
    public VisitSummaryResponse getSummary(@PathVariable UUID visitId) {
        return mapper.toResponse(getVisitSummaryUseCase.getSummary(visitId));
    }

    @GetMapping("/{visitId}/summary/print")
    @RequirePermission("VISIT_SUMMARY_PRINT")
    public ResponseEntity<ByteArrayResource> printSummary(@PathVariable UUID visitId) {
        var printResult = exportVisitSummaryUseCase.export(visitId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + printResult.fileName() + "\"")
                .contentType(MediaType.parseMediaType(printResult.contentType()))
                .contentLength(printResult.content().length)
                .body(new ByteArrayResource(printResult.content()));
    }
}
