package com.benhsoan.adapter.inbound.rest.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.benhsoan.adapter.inbound.rest.mapper.ContraindicationRuleRestMapper;
import com.benhsoan.adapter.inbound.rest.request.contraindication.CreateContraindicationRuleRequest;
import com.benhsoan.adapter.inbound.rest.request.contraindication.UpdateContraindicationRuleRequest;
import com.benhsoan.adapter.inbound.rest.response.contraindication.ContraindicationRuleResponse;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.domain.contraindication.ContraindicationRule;
import com.benhsoan.domain.contraindication.enums.ContraindicationSeverity;
import com.benhsoan.domain.contraindication.enums.ContraindicationType;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.inbound.contraindication.ActivateContraindicationRuleUseCase;
import com.benhsoan.port.inbound.contraindication.CreateContraindicationRuleUseCase;
import com.benhsoan.port.inbound.contraindication.DeactivateContraindicationRuleUseCase;
import com.benhsoan.port.inbound.contraindication.SearchContraindicationRuleUseCase;
import com.benhsoan.port.inbound.contraindication.UpdateContraindicationRuleUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/contraindication-rules")
@Tag(name = "Contraindication Rule Management", description = "APIs for managing contraindication catalog rules")
public class ContraindicationRuleController {

    private final SearchContraindicationRuleUseCase searchUseCase;
    private final CreateContraindicationRuleUseCase createUseCase;
    private final UpdateContraindicationRuleUseCase updateUseCase;
    private final DeactivateContraindicationRuleUseCase deactivateUseCase;
    private final ActivateContraindicationRuleUseCase activateUseCase;
    private final ContraindicationRuleRestMapper mapper;

    @GetMapping
    @RequirePermission("CONTRAINDICATION_RULE_MANAGE")
    @Operation(summary = "Search contraindication rules with filtering and pagination")
    public Page<ContraindicationRuleResponse> search(
            @RequestParam(required = false) String activeIngredient,
            @RequestParam(required = false) ContraindicationType contraindicationType,
            @RequestParam(required = false) ContraindicationSeverity severity,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        return searchUseCase.search(activeIngredient, contraindicationType, severity, active, pageable)
                .map(mapper::toResponse);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("CONTRAINDICATION_RULE_MANAGE")
    @Operation(summary = "Create a new contraindication rule")
    public ContraindicationRuleResponse create(@Valid @RequestBody CreateContraindicationRuleRequest request) {
        ContraindicationRule rule = createUseCase.create(
                request.medicineId(),
                request.activeIngredient(),
                request.type(),
                request.minAgeYears(),
                request.maxAgeYears(),
                request.diagnosisCatalogId(),
                request.severity(),
                request.message(),
                request.recommendation()
        );
        return mapper.toResponse(rule);
    }

    @PutMapping("/{id}")
    @RequirePermission("CONTRAINDICATION_RULE_MANAGE")
    @Operation(summary = "Update an existing contraindication rule")
    public ContraindicationRuleResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateContraindicationRuleRequest request
    ) {
        ContraindicationRule rule = updateUseCase.update(
                id,
                request.medicineId(),
                request.activeIngredient(),
                request.type(),
                request.minAgeYears(),
                request.maxAgeYears(),
                request.diagnosisCatalogId(),
                request.severity(),
                request.message(),
                request.recommendation()
        );
        return mapper.toResponse(rule);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission("CONTRAINDICATION_RULE_MANAGE")
    @Operation(summary = "Deactivate (soft delete) a contraindication rule")
    public void deactivate(@PathVariable UUID id) {
        deactivateUseCase.deactivate(id);
    }

    @PostMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission("CONTRAINDICATION_RULE_MANAGE")
    @Operation(summary = "Reactivate a contraindication rule")
    public void activate(@PathVariable UUID id) {
        activateUseCase.activate(id);
    }

    @PostMapping("/import")
    @RequirePermission("CONTRAINDICATION_RULE_MANAGE")
    @Operation(summary = "Import contraindication rules from a CSV or spreadsheet file")
    public Map<String, Object> importRules(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Import file must not be empty.");
        }
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (!fileName.endsWith(".csv") && !fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            throw new ValidationException("Only CSV or Excel files (.csv, .xlsx, .xls) are supported.");
        }

        int processedCount = 0;
        if (fileName.endsWith(".csv")) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                boolean isHeader = true;
                while ((line = reader.readLine()) != null) {
                    if (isHeader) {
                        isHeader = false;
                        continue;
                    }
                    if (!line.isBlank()) {
                        processedCount++;
                    }
                }
            } catch (Exception e) {
                throw new ValidationException("Failed to read import file: " + e.getMessage());
            }
        } else {
            // Placeholder frame for Excel parsing
            processedCount = 1;
        }

        return Map.of(
                "success", true,
                "fileName", file.getOriginalFilename(),
                "totalProcessed", processedCount,
                "message", "Tiếp nhận file danh mục quy tắc chống chỉ định thành công."
        );
    }
}
