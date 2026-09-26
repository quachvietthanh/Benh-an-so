package com.benhsoan.adapter.inbound.rest.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.SpecialtyRestMapper;
import com.benhsoan.adapter.inbound.rest.request.specialty.CreateSpecialtyRequest;
import com.benhsoan.adapter.inbound.rest.request.specialty.UpdateSpecialtyRequest;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.SpecialtyResponse;
import com.benhsoan.adapter.inbound.rest.response.specialty.SpecialtyDetailResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.command.specialty.DeactivateSpecialtyCommand;
import com.benhsoan.port.inbound.specialty.ActivateSpecialtyUseCase;
import com.benhsoan.port.inbound.specialty.CreateSpecialtyUseCase;
import com.benhsoan.port.inbound.specialty.DeactivateSpecialtyUseCase;
import com.benhsoan.port.inbound.specialty.GetSpecialtyDetailUseCase;
import com.benhsoan.port.inbound.specialty.SearchSpecialtyUseCase;
import com.benhsoan.port.inbound.specialty.UpdateSpecialtyUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/system/specialties")
public class SpecialtyController {

    private final SearchSpecialtyUseCase searchSpecialtyUseCase;
    private final GetSpecialtyDetailUseCase getSpecialtyDetailUseCase;
    private final CreateSpecialtyUseCase createSpecialtyUseCase;
    private final UpdateSpecialtyUseCase updateSpecialtyUseCase;
    private final DeactivateSpecialtyUseCase deactivateSpecialtyUseCase;
    private final ActivateSpecialtyUseCase activateSpecialtyUseCase;
    private final SpecialtyRestMapper mapper;

    @GetMapping
    public List<SpecialtyResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active
    ) {
        return searchSpecialtyUseCase.search(keyword, active).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @RequirePermission("SPECIALTY_MANAGE")
    public SpecialtyDetailResponse getById(@PathVariable UUID id) {
        return mapper.toResponse(getSpecialtyDetailUseCase.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("SPECIALTY_MANAGE")
    public SpecialtyDetailResponse create(@Valid @RequestBody CreateSpecialtyRequest request) {
        return mapper.toResponse(createSpecialtyUseCase.create(mapper.toCommand(request)));
    }

    @PutMapping("/{id}")
    @RequirePermission("SPECIALTY_MANAGE")
    public SpecialtyDetailResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSpecialtyRequest request
    ) {
        return mapper.toResponse(updateSpecialtyUseCase.update(mapper.toCommand(id, request)));
    }

    @PatchMapping("/{id}/deactivate")
    @RequirePermission("SPECIALTY_MANAGE")
    public SpecialtyResponse deactivate(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean confirm
    ) {
        return mapper.toResponse(deactivateSpecialtyUseCase.deactivate(new DeactivateSpecialtyCommand(id, confirm)));
    }

    @PatchMapping("/{id}/activate")
    @RequirePermission("SPECIALTY_MANAGE")
    public SpecialtyResponse activate(@PathVariable UUID id) {
        return mapper.toResponse(activateSpecialtyUseCase.activate(id));
    }
}
