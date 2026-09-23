package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.response.portal.PatientPortalNotificationResponse;
import com.benhsoan.port.dto.result.portal.PatientPortalNotificationResult;

@Component
public class PatientPortalNotificationRestMapper {

    public PatientPortalNotificationResponse toResponse(PatientPortalNotificationResult result) {
        if (result == null) {
            return null;
        }
        return new PatientPortalNotificationResponse(
                result.id(),
                result.type(),
                result.title(),
                result.message(),
                result.read(),
                result.readAt(),
                result.createdAt());
    }
}
