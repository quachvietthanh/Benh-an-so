package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.auditlog.AdminOperationLogResponse;
import com.benhsoan.port.dto.result.auditlog.AdminOperationLogResult;

@Component
public class AdminOperationLogRestMapper {

    public AdminOperationLogResponse toResponse(AdminOperationLogResult result) {
        return new AdminOperationLogResponse(
                result.id(),
                result.actorId(),
                result.actorName(),
                result.actionType(),
                result.resourceType(),
                result.resourceId(),
                result.detail(),
                result.createdAt());
    }

    public Page<AdminOperationLogResponse> toResponse(Page<AdminOperationLogResult> page) {
        return page.map(this::toResponse);
    }
}
