package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;

public record PendingClinicalOrderResult(
        UUID orderItemId,
        UUID orderId,
        String orderCode,
        UUID visitId,
        String visitCode,
        UUID patientId,
        String patientCode,
        String patientFullName,
        UUID doctorId,
        String doctorFullName,
        UUID clinicalServiceId,
        String serviceCode,
        String serviceName,
        ClinicalServiceType serviceType,
        String instruction,
        String clinicalReason,
        ClinicalOrderItemStatus status,
        Instant orderedAt,
        long waitingMinutes
) {
}
