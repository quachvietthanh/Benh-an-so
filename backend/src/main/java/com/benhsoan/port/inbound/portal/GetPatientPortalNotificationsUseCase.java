package com.benhsoan.port.inbound.portal;

import java.util.List;

import com.benhsoan.port.dto.result.portal.PatientPortalNotificationResult;

public interface GetPatientPortalNotificationsUseCase {

    List<PatientPortalNotificationResult> getNotifications(int limit);
}
