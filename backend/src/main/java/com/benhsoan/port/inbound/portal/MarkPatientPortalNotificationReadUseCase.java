package com.benhsoan.port.inbound.portal;

import java.util.UUID;

import com.benhsoan.port.dto.result.portal.PatientPortalNotificationResult;

public interface MarkPatientPortalNotificationReadUseCase {

    PatientPortalNotificationResult markRead(UUID id);
}
