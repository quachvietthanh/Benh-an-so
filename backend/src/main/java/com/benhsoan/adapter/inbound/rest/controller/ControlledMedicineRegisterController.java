package com.benhsoan.adapter.inbound.rest.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.ControlledMedicineRegisterRestMapper;
import com.benhsoan.adapter.inbound.rest.response.controlledmedicine.ControlledMedicineRegisterResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.command.controlledmedicine.SearchControlledMedicineRegisterQuery;
import com.benhsoan.port.inbound.controlledmedicine.SearchControlledMedicineRegisterUseCase;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/controlled-medicines/register")
public class ControlledMedicineRegisterController {

    private final SearchControlledMedicineRegisterUseCase searchUseCase;
    private final ControlledMedicineRegisterRestMapper restMapper;

    @GetMapping
    @RequirePermission("CONTROLLED_MEDICINE_REGISTER_READ")
    public Page<ControlledMedicineRegisterResponse> search(
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID medicineId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return restMapper.toResponse(
                searchUseCase.search(new SearchControlledMedicineRegisterQuery(
                        patientId,
                        medicineId,
                        from,
                        to,
                        page,
                        size
                ))
        );
    }
}
