package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.PatientPortalClinicalResultRestMapper;
import com.benhsoan.adapter.inbound.rest.response.portal.PatientPortalClinicalResultDetailResponse;
import com.benhsoan.adapter.inbound.rest.response.portal.PatientPortalClinicalResultSummaryResponse;
import com.benhsoan.port.inbound.portal.ExportPatientPortalClinicalResultUseCase;
import com.benhsoan.port.inbound.portal.GetPatientPortalClinicalResultDetailUseCase;
import com.benhsoan.port.inbound.portal.GetPatientPortalClinicalResultsUseCase;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-009: Controller for patient portal clinical results.
 * Allows authenticated patients to view confirmed clinical results for their visits (TC-01, TC-04),
 * hides unconfirmed draft results (TC-02), denies cross-patient access (TC-03), and provides
 * readable PDF downloads (TC-01).
 */
@RestController
@RequestMapping("/patient-portal")
@RequiredArgsConstructor
public class PatientPortalClinicalResultController {

    private final GetPatientPortalClinicalResultsUseCase getPatientPortalClinicalResultsUseCase;
    private final GetPatientPortalClinicalResultDetailUseCase getPatientPortalClinicalResultDetailUseCase;
    private final ExportPatientPortalClinicalResultUseCase exportPatientPortalClinicalResultUseCase;
    private final PatientPortalClinicalResultRestMapper mapper;

    @GetMapping("/clinical-results")
    public List<PatientPortalClinicalResultSummaryResponse> getClinicalResults(
            @RequestParam UUID visitId
    ) {
        return getPatientPortalClinicalResultsUseCase.getClinicalResults(visitId).stream()
                .map(mapper::toSummaryResponse)
                .toList();
    }

    @GetMapping("/visits/{visitId}/clinical-results")
    public List<PatientPortalClinicalResultSummaryResponse> getClinicalResultsByVisit(
            @PathVariable UUID visitId
    ) {
        return getPatientPortalClinicalResultsUseCase.getClinicalResults(visitId).stream()
                .map(mapper::toSummaryResponse)
                .toList();
    }

    @GetMapping("/clinical-results/{resultId}")
    public PatientPortalClinicalResultDetailResponse getClinicalResultDetail(
            @PathVariable UUID resultId
    ) {
        return mapper.toDetailResponse(getPatientPortalClinicalResultDetailUseCase.getClinicalResultDetail(resultId));
    }

    @GetMapping("/clinical-results/{resultId}/download")
    public ResponseEntity<ByteArrayResource> downloadResult(
            @PathVariable UUID resultId
    ) {
        var printResult = exportPatientPortalClinicalResultUseCase.exportByResult(resultId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + printResult.fileName() + "\"")
                .contentType(MediaType.parseMediaType(printResult.contentType()))
                .contentLength(printResult.content().length)
                .body(new ByteArrayResource(printResult.content()));
    }

    @GetMapping("/visits/{visitId}/clinical-results/download")
    public ResponseEntity<ByteArrayResource> downloadVisitResults(
            @PathVariable UUID visitId
    ) {
        var printResult = exportPatientPortalClinicalResultUseCase.exportByVisit(visitId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + printResult.fileName() + "\"")
                .contentType(MediaType.parseMediaType(printResult.contentType()))
                .contentLength(printResult.content().length)
                .body(new ByteArrayResource(printResult.content()));
    }
}
