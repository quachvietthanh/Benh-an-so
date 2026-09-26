package com.benhsoan.adapter.inbound.rest.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.benhsoan.adapter.inbound.rest.mapper.AdminOperationLogRestMapper;
import com.benhsoan.adapter.inbound.rest.response.auditlog.AdminOperationLogResponse;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.infrastructure.security.annotation.RequirePermission;
import com.benhsoan.port.dto.command.auditlog.AdminOperationLogQuery;
import com.benhsoan.port.inbound.auditlog.GetAdminOperationLogsUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin-operation-logs")
@RequiredArgsConstructor
public class AdminOperationLogController {

    private final GetAdminOperationLogsUseCase getAdminOperationLogsUseCase;
    private final AdminOperationLogRestMapper mapper;

    @GetMapping
    @RequirePermission("ADMIN_OPERATION_LOG_READ")
    public Page<AdminOperationLogResponse> getLogs(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) ResourceType resourceType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        validateRange(from, to);
        return mapper.toResponse(getAdminOperationLogsUseCase.getLogs(
                new AdminOperationLogQuery(actorId, resourceType, from, to), pageable));
    }

    private void validateRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationException("from must be before or equal to to.");
        }
    }
}
