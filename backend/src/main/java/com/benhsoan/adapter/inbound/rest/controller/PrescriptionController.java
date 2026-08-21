package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.PrescriptionRestMapper;
import com.benhsoan.adapter.inbound.rest.request.prescription.AmendPrescriptionRequest;
import com.benhsoan.adapter.inbound.rest.request.prescription.CheckDrugInteractionRequest;
import com.benhsoan.adapter.inbound.rest.request.prescription.CreatePrescriptionRequest;
import com.benhsoan.adapter.inbound.rest.response.prescription.DrugInteractionWarningResponse;
import com.benhsoan.adapter.inbound.rest.response.prescription.DispensePrescriptionResponse;
import com.benhsoan.adapter.inbound.rest.response.prescription.PrescriptionResponse;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.command.prescription.SearchPrescriptionsQuery;
import com.benhsoan.port.dto.result.PrescriptionResult;
import com.benhsoan.port.inbound.prescription.AmendPrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.CancelPrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.CheckDrugInteractionUseCase;
import com.benhsoan.port.inbound.prescription.CreatePrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.DispensePrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.ExportPrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.GetPrescriptionUseCase;
import com.benhsoan.port.inbound.prescription.GetPrescriptionsByMedicalRecordUseCase;
import com.benhsoan.port.inbound.prescription.SearchPrescriptionsUseCase;
import com.benhsoan.port.inbound.prescription.SendPrescriptionInterconnectionUseCase;
import com.benhsoan.port.inbound.prescription.RetryPrescriptionInterconnectionUseCase;
import com.benhsoan.port.dto.result.PrescriptionInterconnectionResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/prescriptions")
@RequiredArgsConstructor
@Validated
public class PrescriptionController {

    private final CreatePrescriptionUseCase createPrescriptionUseCase;

    private final AmendPrescriptionUseCase amendPrescriptionUseCase;
    private final GetPrescriptionUseCase getPrescriptionUseCase;
    private final GetPrescriptionsByMedicalRecordUseCase getPrescriptionsByMedicalRecordUseCase;
    private final SearchPrescriptionsUseCase searchPrescriptionsUseCase;
    private final DispensePrescriptionUseCase dispensePrescriptionUseCase;
    private final CancelPrescriptionUseCase cancelPrescriptionUseCase;
    private final CheckDrugInteractionUseCase checkDrugInteractionUseCase;
    private final ExportPrescriptionUseCase exportPrescriptionUseCase;
    private final SendPrescriptionInterconnectionUseCase sendPrescriptionInterconnectionUseCase;
    private final RetryPrescriptionInterconnectionUseCase retryPrescriptionInterconnectionUseCase;

    private final PrescriptionRestMapper mapper;

    @GetMapping
    @RequirePermission("PRESCRIPTION_READ")
    public Page<PrescriptionResponse> search(
            @RequestParam PrescriptionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return searchPrescriptionsUseCase.search(new SearchPrescriptionsQuery(status, page, size))
                .map(mapper::toResponse);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("PRESCRIPTION_CREATE")
    public PrescriptionResponse create(
            @Valid @RequestBody CreatePrescriptionRequest request
    ) {
        PrescriptionResult result = createPrescriptionUseCase.create(
                mapper.toCommand(request)
        );

        return mapper.toResponse(result);
    }

    @PatchMapping("/{id}")
    @RequirePermission("PRESCRIPTION_UPDATE")
    public PrescriptionResponse amend(
            @PathVariable UUID id,
            @Valid @RequestBody AmendPrescriptionRequest request
    ) {
        PrescriptionResult result = amendPrescriptionUseCase.amend(
                mapper.toCommand(id, request)
        );

        return mapper.toResponse(result);
    }

    @GetMapping("/{id}")
    @RequirePermission("PRESCRIPTION_READ")
    public PrescriptionResponse getById(@PathVariable UUID id) {
        return mapper.toResponse(getPrescriptionUseCase.getById(id));
    }

    @GetMapping("/{id}/print")
    @RequirePermission("PRESCRIPTION_PRINT")
    public ResponseEntity<ByteArrayResource> print(@PathVariable UUID id) {
        var printResult = exportPrescriptionUseCase.export(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + printResult.fileName() + "\"")
                .contentType(MediaType.parseMediaType(printResult.contentType()))
                .contentLength(printResult.content().length)
                .body(new ByteArrayResource(printResult.content()));
    }

    @GetMapping("/medical-records/{medicalRecordId}")
    @RequirePermission("PRESCRIPTION_READ")
    public java.util.List<PrescriptionResponse> getByMedicalRecordId(
            @PathVariable UUID medicalRecordId
    ) {
        return getPrescriptionsByMedicalRecordUseCase.getByMedicalRecordId(medicalRecordId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @PostMapping("/{id}/dispense")
    @RequirePermission("PRESCRIPTION_UPDATE_STATUS")
    public DispensePrescriptionResponse dispense(@PathVariable UUID id) {
        return mapper.toResponse(dispensePrescriptionUseCase.dispense(id));
    }

    @PostMapping("/{id}/cancel")
    @RequirePermission("PRESCRIPTION_UPDATE")
    public PrescriptionResponse cancel(@PathVariable UUID id) {
        return mapper.toResponse(cancelPrescriptionUseCase.cancel(id));
    }

    @PostMapping("/{id}/interconnection")
    @RequirePermission("PRESCRIPTION_INTERCONNECTION_SEND")
    @Operation(summary = "Send a prescription to the interconnection gateway")
    @ApiResponse(responseCode = "200", description = "Submission result")
    @ApiResponse(responseCode = "403", description = "Requires interconnection send permission")
    public PrescriptionInterconnectionResult sendToInterconnection(@PathVariable UUID id) {
        return sendPrescriptionInterconnectionUseCase.send(id);
    }

    @PostMapping("/{id}/interconnection/retry")
    @RequirePermission("PRESCRIPTION_INTERCONNECTION_RETRY")
    @Operation(summary = "Retry a failed prescription interconnection submission")
    @ApiResponse(responseCode = "200", description = "Retry result")
    @ApiResponse(responseCode = "403", description = "Requires interconnection retry permission")
    public PrescriptionInterconnectionResult retryInterconnection(@PathVariable UUID id) {
        return retryPrescriptionInterconnectionUseCase.retry(id);
    }

    @PostMapping("/check-interactions")
    @RequirePermission("PRESCRIPTION_CREATE")
    public List<DrugInteractionWarningResponse> checkInteractions(
            @Valid @RequestBody CheckDrugInteractionRequest request
    ) {
        return mapper.toResponse(
                checkDrugInteractionUseCase.check(mapper.toCommand(request))
        );
    }
}
