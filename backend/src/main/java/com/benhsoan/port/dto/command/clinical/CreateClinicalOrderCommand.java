package com.benhsoan.port.dto.command.clinical;

import java.util.List;
import java.util.UUID;

import com.benhsoan.domain.shared.exception.ValidationException;

public record CreateClinicalOrderCommand(
        String clinicalReason,
        List<OrderItemCommand> items
) {
    public CreateClinicalOrderCommand {
        if (items == null || items.isEmpty()) {
            throw new ValidationException("At least one clinical service is required.");
        }
        items = List.copyOf(items);
    }

    public record OrderItemCommand(UUID serviceId, String instruction) {
        public OrderItemCommand {
            if (serviceId == null) {
                throw new ValidationException("Clinical service id is required.");
            }
        }
    }
}
