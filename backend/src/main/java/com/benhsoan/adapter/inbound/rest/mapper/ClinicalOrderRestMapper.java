package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.clinical.CreateClinicalOrderRequest;
import com.benhsoan.adapter.inbound.rest.response.clinical.ClinicalOrderResponse;
import com.benhsoan.port.dto.command.clinical.CreateClinicalOrderCommand;
import com.benhsoan.port.dto.result.ClinicalOrderResult;

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
                        item.id(), item.serviceCode(), item.serviceName(), item.instruction(), item.status()
                ))
                .toList();
        return new ClinicalOrderResponse(
                result.id(), result.orderCode(), result.visitId(), result.patientId(), result.orderedBy(),
                result.clinicalReason(), result.status(), result.orderedAt(), result.completedAt(), items
        );
    }
}
