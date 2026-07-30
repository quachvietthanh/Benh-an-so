package com.benhsoan.port.dto.command.clinical;

import java.util.List;
import java.util.UUID;

public record CreateClinicalOrderCommand(
        String clinicalReason,
        List<OrderItemCommand> items
) {
    public record OrderItemCommand(
            UUID serviceId,
            String serviceCode,
            String serviceName,
            String instruction
    ) {}
}
