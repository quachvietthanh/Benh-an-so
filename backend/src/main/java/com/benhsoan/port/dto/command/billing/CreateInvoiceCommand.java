package com.benhsoan.port.dto.command.billing;

import java.util.UUID;

import lombok.Builder;

@Builder
public record CreateInvoiceCommand(
        UUID visitId,
        UUID paymentId
) {
}
