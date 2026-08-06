package com.benhsoan.adapter.inbound.rest.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.benhsoan.adapter.inbound.rest.request.medicine.CreateMedicineRequest;
import com.benhsoan.adapter.inbound.rest.request.medicine.UpdateMedicineRequest;
import com.benhsoan.adapter.inbound.rest.request.medicine.UpdateMedicineStatusRequest;
import com.benhsoan.adapter.inbound.rest.mapper.MedicineRestMapper;
import com.benhsoan.adapter.inbound.rest.response.medicine.MedicineResponse;
import com.benhsoan.port.dto.command.medicine.SearchMedicinesQuery;
import com.benhsoan.port.dto.result.MedicineResult;
import com.benhsoan.port.inbound.medicine.ActivateMedicineUseCase;
import com.benhsoan.port.inbound.medicine.CreateMedicineUseCase;
import com.benhsoan.port.inbound.medicine.DeactivateMedicineUseCase;
import com.benhsoan.port.inbound.medicine.GetMedicineUseCase;
import com.benhsoan.port.inbound.medicine.SearchMedicinesUseCase;
import com.benhsoan.port.inbound.medicine.UpdateMedicineUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/medicines")
public class MedicineController {

    private final CreateMedicineUseCase createMedicineUseCase;

    private final UpdateMedicineUseCase updateMedicineUseCase;

    private final ActivateMedicineUseCase activateMedicineUseCase;

    private final DeactivateMedicineUseCase deactivateMedicineUseCase;

    private final GetMedicineUseCase getMedicineUseCase;

    private final SearchMedicinesUseCase searchMedicinesUseCase;

    private final MedicineRestMapper restMapper;

    @PostMapping
    public ResponseEntity<MedicineResponse> create(
            @Valid @RequestBody CreateMedicineRequest request
    ) {
        MedicineResponse response = restMapper.toResponse(
                createMedicineUseCase.create(restMapper.toCommand(request))
        );
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{medicineId}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{medicineId}")
    public ResponseEntity<MedicineResponse> update(
            @PathVariable UUID medicineId,
            @Valid @RequestBody UpdateMedicineRequest request
    ) {
        return ResponseEntity.ok(restMapper.toResponse(
                updateMedicineUseCase.update(
                        restMapper.toCommand(medicineId, request)
                )
        ));
    }

    @PatchMapping("/{medicineId}/status")
    public ResponseEntity<MedicineResponse> updateStatus(
            @PathVariable UUID medicineId,
            @Valid @RequestBody UpdateMedicineStatusRequest request
    ) {
        MedicineResult result = request.active()
                ? activateMedicineUseCase.activate(medicineId)
                : deactivateMedicineUseCase.deactivate(medicineId);

        return ResponseEntity.ok(restMapper.toResponse(result));
    }

    @GetMapping("/{medicineId}")
    public ResponseEntity<MedicineResponse> getById(
            @PathVariable UUID medicineId
    ) {
        return ResponseEntity.ok(restMapper.toResponse(
                getMedicineUseCase.getById(medicineId)
        ));
    }

    @GetMapping
    public Page<MedicineResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(
                    size = 20,
                    sort = {"medicineName", "medicineCode"}
            ) Pageable pageable
    ) {
        SearchMedicinesQuery query = new SearchMedicinesQuery(
                keyword,
                active,
                pageable
        );

        return restMapper.toResponse(
                searchMedicinesUseCase.search(query)
        );
    }
}
