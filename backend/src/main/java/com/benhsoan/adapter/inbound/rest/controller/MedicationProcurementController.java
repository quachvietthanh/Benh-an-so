package com.benhsoan.adapter.inbound.rest.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.benhsoan.adapter.inbound.rest.mapper.MedicationProcurementRestMapper;
import com.benhsoan.adapter.inbound.rest.request.inventory.ApproveProcurementPlanRequest;
import com.benhsoan.adapter.inbound.rest.request.inventory.CreateProcurementPlanRequest;
import com.benhsoan.adapter.inbound.rest.request.inventory.RejectProcurementPlanRequest;
import com.benhsoan.adapter.inbound.rest.request.inventory.UpdateProcurementPlanRequest;
import com.benhsoan.adapter.inbound.rest.response.inventory.ProcurementPlanResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.ProcurementPlanSummaryResponse;
import com.benhsoan.adapter.inbound.rest.response.inventory.ProcurementSuggestionResponse;
import com.benhsoan.domain.inventory.procurement.enums.ProcurementPlanStatus;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.query.inventory.ListProcurementPlansQuery;
import com.benhsoan.port.dto.result.ProcurementPlanResult;
import com.benhsoan.port.inbound.inventory.ApproveMedicationProcurementPlanUseCase;
import com.benhsoan.port.inbound.inventory.CreateMedicationProcurementPlanUseCase;
import com.benhsoan.port.inbound.inventory.GetMedicationProcurementPlanUseCase;
import com.benhsoan.port.inbound.inventory.GetMedicationProcurementSuggestionUseCase;
import com.benhsoan.port.inbound.inventory.ListMedicationProcurementPlansUseCase;
import com.benhsoan.port.inbound.inventory.RejectMedicationProcurementPlanUseCase;
import com.benhsoan.port.inbound.inventory.UpdateMedicationProcurementPlanUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/inventory/procurements")
public class MedicationProcurementController {

    private final GetMedicationProcurementSuggestionUseCase suggestionUseCase;
    private final CreateMedicationProcurementPlanUseCase createUseCase;
    private final GetMedicationProcurementPlanUseCase getUseCase;
    private final ListMedicationProcurementPlansUseCase listUseCase;
    private final UpdateMedicationProcurementPlanUseCase updateUseCase;
    private final ApproveMedicationProcurementPlanUseCase approveUseCase;
    private final RejectMedicationProcurementPlanUseCase rejectUseCase;
    private final MedicationProcurementRestMapper restMapper;

    @GetMapping("/suggestions")
    @RequirePermission("MEDICATION_PROCUREMENT_READ")
    public ProcurementSuggestionResponse getSuggestions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "true") boolean onlyBelowThreshold
    ) {
        return restMapper.toSuggestionResponse(
                suggestionUseCase.getSuggestions(from, to, onlyBelowThreshold)
        );
    }

    @PostMapping
    @RequirePermission("MEDICATION_PROCUREMENT_CREATE")
    public ResponseEntity<ProcurementPlanResponse> create(
            @Valid @RequestBody CreateProcurementPlanRequest request
    ) {
        ProcurementPlanResult result = createUseCase.create(restMapper.toCreateCommand(request));
        ProcurementPlanResponse response = restMapper.toPlanResponse(result);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @RequirePermission("MEDICATION_PROCUREMENT_READ")
    public Page<ProcurementPlanSummaryResponse> list(
            @RequestParam(required = false) ProcurementPlanStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ListProcurementPlansQuery query = new ListProcurementPlansQuery(status, from, to, createdBy, page, size);
        return listUseCase.list(query).map(restMapper::toSummaryResponse);
    }

    @GetMapping("/{id}")
    @RequirePermission("MEDICATION_PROCUREMENT_READ")
    public ProcurementPlanResponse getById(@PathVariable UUID id) {
        return restMapper.toPlanResponse(getUseCase.getById(id));
    }

    @PutMapping("/{id}")
    @RequirePermission("MEDICATION_PROCUREMENT_CREATE")
    public ProcurementPlanResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProcurementPlanRequest request
    ) {
        return restMapper.toPlanResponse(
                updateUseCase.update(restMapper.toUpdateCommand(id, request))
        );
    }

    @PostMapping("/{id}/submit")
    @RequirePermission("MEDICATION_PROCUREMENT_CREATE")
    public ProcurementPlanResponse submit(@PathVariable UUID id) {
        return restMapper.toPlanResponse(updateUseCase.submit(id));
    }

    @PostMapping("/{id}/cancel")
    @RequirePermission("MEDICATION_PROCUREMENT_CREATE")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        updateUseCase.cancel(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/approve")
    @RequirePermission("MEDICATION_PROCUREMENT_APPROVE")
    public ProcurementPlanResponse approve(
            @PathVariable UUID id,
            @RequestBody(required = false) ApproveProcurementPlanRequest request
    ) {
        return restMapper.toPlanResponse(
                approveUseCase.approve(restMapper.toApproveCommand(id, request))
        );
    }

    @PostMapping("/{id}/reject")
    @RequirePermission("MEDICATION_PROCUREMENT_APPROVE")
    public ProcurementPlanResponse reject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectProcurementPlanRequest request
    ) {
        return restMapper.toPlanResponse(
                rejectUseCase.reject(restMapper.toRejectCommand(id, request))
        );
    }
}
