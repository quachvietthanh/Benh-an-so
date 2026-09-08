package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.security.SecurityAlertResponse;
import com.benhsoan.port.dto.result.security.SecurityAlertResult;

@Component
public class SecurityAlertRestMapper {

    public Page<SecurityAlertResponse> toResponse(Page<SecurityAlertResult> results) {
        return results.map(this::toResponse);
    }

    public SecurityAlertResponse toResponse(SecurityAlertResult result) {
        return new SecurityAlertResponse(
                result.id(),
                result.userId(),
                result.username(),
                result.fullName(),
                result.alertType(),
                result.severity(),
                result.description(),
                result.accessCount(),
                result.windowStart(),
                result.windowEnd(),
                result.status(),
                result.createdAt());
    }
}
