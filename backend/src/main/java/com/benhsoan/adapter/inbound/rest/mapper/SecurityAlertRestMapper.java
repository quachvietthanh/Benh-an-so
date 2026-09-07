package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.security.SecurityAlertResponse;
import com.benhsoan.port.dto.result.security.SecurityAlertResult;

@Component
public class SecurityAlertRestMapper {

    public SecurityAlertResponse toResponse(SecurityAlertResult result) {
        return new SecurityAlertResponse(
                result.id(),
                result.userId(),
                result.alertType(),
                result.severity(),
                result.description(),
                result.accessCount(),
                result.windowStart(),
                result.windowEnd(),
                result.status(),
                result.createdAt());
    }

    public List<SecurityAlertResponse> toResponse(List<SecurityAlertResult> results) {
        return results.stream().map(this::toResponse).toList();
    }
}
