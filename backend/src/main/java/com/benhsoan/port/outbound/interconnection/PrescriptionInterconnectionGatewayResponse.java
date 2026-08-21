package com.benhsoan.port.outbound.interconnection;

import java.time.Instant;

public record PrescriptionInterconnectionGatewayResponse(
        String receiptCode,
        String status,
        Instant receivedAt
) {
}
