package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.clinical.CreateClinicalOrderRequest;
import com.benhsoan.adapter.inbound.rest.response.clinical.ClinicalOrderResponse;
import com.benhsoan.port.dto.command.clinical.CreateClinicalOrderCommand;
import com.benhsoan.port.dto.result.ClinicalOrderResult;

import com.benhsoan.adapter.inbound.rest.response.clinical.PendingClinicalOrderResponse;
import com.benhsoan.port.dto.result.PendingClinicalOrderResult;

@Component
public class ClinicalOrderRestMapper {

    public CreateClinicalOrderCommand toCommand(CreateClinicalOrderRequest request) {
        var items = request.items().stream()
                .map(item -> new CreateClinicalOrderCommand.OrderItemCommand(
                        item.serviceId(), item.instruction()
                ))
                .toList();
        return new CreateClinicalOrderCommand(request.clinicalReason(), items);
    }

    public ClinicalOrderResponse toResponse(ClinicalOrderResult result) {
        var items = result.items().stream()
                .map(item -> new ClinicalOrderResponse.OrderItemResponse(
                        item.id(), item.serviceCode(), item.serviceName(), item.instruction(), item.status(),
                        item.cancelReason(), item.cancelledAt()
                ))
                .toList();
        return new ClinicalOrderResponse(
                result.id(), result.orderCode(), result.visitId(), result.patientId(), result.orderedBy(),
                result.clinicalReason(), result.status(), result.orderedAt(), result.completedAt(), items,
                result.cancelReason(), result.cancelledAt()
        );
    }

    public PendingClinicalOrderResponse toPendingResponse(PendingClinicalOrderResult result) {
        return new PendingClinicalOrderResponse(
                result.orderItemId(),
                result.orderId(),
                result.orderCode(),
                result.visitId(),
                result.visitCode(),
                result.patientId(),
                result.patientCode(),
                result.patientFullName(),
                result.doctorId(),
                result.doctorFullName(),
                result.clinicalServiceId(),
                result.serviceCode(),
                result.serviceName(),
                result.serviceType() != null ? result.serviceType().name() : null,
                result.instruction(),
                result.clinicalReason(),
                result.status() != null ? result.status().name() : null,
                result.orderedAt(),
                result.waitingMinutes()
        );
    }
}
