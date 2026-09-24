package com.benhsoan.application.ucservice.portal;

import org.springframework.stereotype.Component;

import com.benhsoan.domain.portal.notification.PatientPortalNotification;
import com.benhsoan.port.dto.result.portal.PatientPortalNotificationResult;

@Component
public class PatientPortalNotificationResultMapper {

    public PatientPortalNotificationResult toResult(PatientPortalNotification notification) {
        if (notification == null) {
            return null;
        }
        return new PatientPortalNotificationResult(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt());
    }
}
