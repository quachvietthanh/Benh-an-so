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

import com.benhsoan.adapter.inbound.rest.mapper.PatientPortalInvoiceRestMapper;
import com.benhsoan.adapter.inbound.rest.response.portal.PatientPortalInvoiceDetailResponse;
import com.benhsoan.adapter.inbound.rest.response.portal.PatientPortalInvoiceSummaryResponse;
import com.benhsoan.port.inbound.portal.ExportPatientPortalInvoiceUseCase;
import com.benhsoan.port.inbound.portal.GetPatientPortalInvoiceDetailUseCase;
import com.benhsoan.port.inbound.portal.GetPatientPortalInvoicesUseCase;

import lombok.RequiredArgsConstructor;

/**
 * NCL-14-CN-007: Controller for patient portal invoices.
 * Allows patients to view their list of invoices (TC-01, TC-04), view invoice details (TC-01),
 * and download a readable PDF file (TC-02) while strictly blocking unauthorized access (TC-03).
 */
@RestController
@RequestMapping("/patient-portal/invoices")
@RequiredArgsConstructor
public class PatientPortalInvoiceController {

    private final GetPatientPortalInvoicesUseCase getPatientPortalInvoicesUseCase;
    private final GetPatientPortalInvoiceDetailUseCase getPatientPortalInvoiceDetailUseCase;
    private final ExportPatientPortalInvoiceUseCase exportPatientPortalInvoiceUseCase;
    private final PatientPortalInvoiceRestMapper mapper;

    @GetMapping
    public List<PatientPortalInvoiceSummaryResponse> getInvoices(
            @RequestParam(required = false) UUID visitId,
            @RequestParam(required = false) Integer limit
    ) {
        return getPatientPortalInvoicesUseCase.getInvoices(visitId, limit).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/{invoiceId}")
    public PatientPortalInvoiceDetailResponse getInvoiceDetail(@PathVariable UUID invoiceId) {
        return mapper.toResponse(getPatientPortalInvoiceDetailUseCase.getInvoiceDetail(invoiceId));
    }

    @GetMapping("/{invoiceId}/download")
    public ResponseEntity<ByteArrayResource> downloadInvoice(@PathVariable UUID invoiceId) {
        var printResult = exportPatientPortalInvoiceUseCase.export(invoiceId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + printResult.fileName() + "\"")
                .contentType(MediaType.parseMediaType(printResult.contentType()))
                .contentLength(printResult.content().length)
                .body(new ByteArrayResource(printResult.content()));
    }
}
