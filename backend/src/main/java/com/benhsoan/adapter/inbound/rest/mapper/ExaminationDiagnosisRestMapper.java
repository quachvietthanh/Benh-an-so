package com.benhsoan.adapter.inbound.rest.mapper;



import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.medicalrecord.RecordDiagnosisRequest;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.ExaminationDiagnosisResponse;
import com.benhsoan.port.dto.command.medicalrecord.RecordDiagnosisCommand;
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

}
