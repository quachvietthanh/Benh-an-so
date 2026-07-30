package com.benhsoan.adapter.inbound.rest.mapper;



import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.clinical.CreateClinicalOrderRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.RecordDiagnosisRequest;
import com.benhsoan.adapter.inbound.rest.response.clinical.ClinicalOrderResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.ExaminationDiagnosisResponse;
import com.benhsoan.port.dto.command.clinical.CreateClinicalOrderCommand;
import com.benhsoan.port.dto.command.medicalrecord.RecordDiagnosisCommand;
import com.benhsoan.port.dto.result.ClinicalOrderResult;
import com.benhsoan.port.dto.result.ExaminationDiagnosisResult;

@Component
public class ExaminationDiagnosisRestMapper {

    public RecordDiagnosisCommand toCommand(RecordDiagnosisRequest request) {
        var secondary = (request.secondaryIcdCodes() != null)
                ? request.secondaryIcdCodes().stream()
                        .map(s -> new RecordDiagnosisCommand.SecondaryIcd(s.code(), s.name()))
                        .toList()
                : null;
        return new RecordDiagnosisCommand(
                request.diagnosisCatalogId(),
                request.primaryIcdCode(),
                request.primaryIcdName(),
                secondary,
                request.clinicalNotes()
        );
    }

    public CreateClinicalOrderCommand toCommand(CreateClinicalOrderRequest request) {
        var items = request.items().stream()
                .map(i -> new CreateClinicalOrderCommand.OrderItemCommand(
                        i.serviceId(), i.serviceCode(), i.serviceName(), i.instruction()))
                .toList();
        return new CreateClinicalOrderCommand(request.clinicalReason(), items);
    }

    public ExaminationDiagnosisResponse toResponse(ExaminationDiagnosisResult result) {
        var secondary = result.secondaryDiagnoses().stream()
                .map(s -> new ExaminationDiagnosisResponse.SecondaryDiagnosisResponse(s.id(), s.code(), s.name()))
                .toList();
        var orders = result.clinicalOrders().stream()
                .map(o -> new ExaminationDiagnosisResponse.ClinicalOrderSummaryResponse(
                        o.id(), o.orderCode(), o.serviceCode(), o.serviceName(), o.status(), o.orderedAt()))
                .toList();
        return new ExaminationDiagnosisResponse(
                result.id(), result.visitId(), result.doctorId(),
                result.primaryIcdCode(), result.primaryIcdName(),
                secondary, result.clinicalNotes(), result.diagnosedAt(), orders
        );
    }

    public ClinicalOrderResponse toResponse(ClinicalOrderResult result) {
        var items = result.items().stream()
                .map(i -> new ClinicalOrderResponse.OrderItemResponse(
                        i.id(), i.serviceCode(), i.serviceName(), i.instruction(), i.status()))
                .toList();
        return new ClinicalOrderResponse(
                result.id(), result.orderCode(), result.visitId(), result.patientId(),
                result.orderedBy(), result.clinicalReason(), result.status(),
                result.orderedAt(), result.completedAt(), items
        );
    }
}
