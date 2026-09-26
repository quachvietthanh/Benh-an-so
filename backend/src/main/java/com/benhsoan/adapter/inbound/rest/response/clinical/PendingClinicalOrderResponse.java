package com.benhsoan.adapter.inbound.rest.response.clinical;

import java.time.Instant;
import java.util.UUID;

public record PendingClinicalOrderResponse(
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
        String serviceType,
        String instruction,
        String clinicalReason,
        String status,
        Instant orderedAt,
        long waitingMinutes
) {
}
