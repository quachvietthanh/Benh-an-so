package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.ClinicalResultRestMapper;
import com.benhsoan.adapter.inbound.rest.request.clinical.EnterClinicalResultRequest;
import com.benhsoan.adapter.inbound.rest.request.clinical.UpdateClinicalResultRequest;
import com.benhsoan.adapter.inbound.rest.response.clinical.ClinicalResultResponse;
import com.benhsoan.port.dto.command.clinical.GetClinicalResultsByVisitQuery;
import com.benhsoan.port.inbound.clinical.EnterClinicalResultUseCase;
import com.benhsoan.port.inbound.clinical.FinalizeClinicalResultUseCase;
import com.benhsoan.port.inbound.clinical.GetClinicalResultHistoryUseCase;
import com.benhsoan.port.inbound.clinical.GetClinicalResultUseCase;
import com.benhsoan.port.inbound.clinical.GetClinicalResultsByVisitUseCase;
import com.benhsoan.port.inbound.clinical.UpdateClinicalResultUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Validated
public class ClinicalResultController {

    private final EnterClinicalResultUseCase enterClinicalResultUseCase;
    private final UpdateClinicalResultUseCase updateClinicalResultUseCase;
    private final FinalizeClinicalResultUseCase finalizeClinicalResultUseCase;
    private final GetClinicalResultUseCase getClinicalResultUseCase;
    private final GetClinicalResultsByVisitUseCase getClinicalResultsByVisitUseCase;
    private final GetClinicalResultHistoryUseCase getClinicalResultHistoryUseCase;
    private final ClinicalResultRestMapper mapper;

    @PostMapping("/clinical-order-items/{itemId}/results")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ClinicalResultResponse enter(@PathVariable UUID itemId, @Valid @RequestBody EnterClinicalResultRequest request) {
        return mapper.toResponse(enterClinicalResultUseCase.enter(itemId, mapper.toCommand(request)));
    }

    @PutMapping("/clinical-results/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ClinicalResultResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateClinicalResultRequest request) {
        return mapper.toResponse(updateClinicalResultUseCase.update(id, mapper.toCommand(request)));
    }

    @PostMapping("/clinical-results/{id}/finalize")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ClinicalResultResponse finalizeResult(@PathVariable UUID id) {
        return mapper.toResponse(finalizeClinicalResultUseCase.finalizeResult(id));
    }

    @GetMapping("/clinical-results/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public ClinicalResultResponse getById(@PathVariable UUID id) {
        return mapper.toResponse(getClinicalResultUseCase.getById(id));
    }

    @GetMapping("/clinical-results/visits/{visitId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public Page<ClinicalResultResponse> getByVisit(@PathVariable UUID visitId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return getClinicalResultsByVisitUseCase.getResultsByVisit(new GetClinicalResultsByVisitQuery(visitId, page, size))
                .map(mapper::toResponse);
    }

    @GetMapping("/clinical-results/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public List<ClinicalResultResponse.HistoryResponse> getHistory(@PathVariable UUID id) {
        return getClinicalResultHistoryUseCase.getHistory(id).stream().map(mapper::toHistoryResponse).toList();
    }
}
