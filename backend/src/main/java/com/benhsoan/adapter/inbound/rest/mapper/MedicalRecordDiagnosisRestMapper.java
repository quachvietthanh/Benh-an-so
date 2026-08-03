package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.medicalrecord.ReplaceMedicalRecordDiagnosesRequest;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordDiagnosisResponse;
import com.benhsoan.port.dto.command.medicalrecord.ReplaceMedicalRecordDiagnosesCommand;
import com.benhsoan.port.dto.result.MedicalRecordDiagnosisResult;

@Component
public class MedicalRecordDiagnosisRestMapper {

    public ReplaceMedicalRecordDiagnosesCommand toCommand(ReplaceMedicalRecordDiagnosesRequest request) {
        List<ReplaceMedicalRecordDiagnosesCommand.DiagnosisCommand> secondaryDiagnoses = request.secondaryDiagnoses() == null
                ? List.of()
                : request.secondaryDiagnoses().stream().map(this::toCommand).toList();
        return new ReplaceMedicalRecordDiagnosesCommand(toCommand(request.primaryDiagnosis()), secondaryDiagnoses);
    }

    public List<MedicalRecordDiagnosisResponse> toResponses(List<MedicalRecordDiagnosisResult> results) {
        return results.stream().map(this::toResponse).toList();
    }

    private ReplaceMedicalRecordDiagnosesCommand.DiagnosisCommand toCommand(
            ReplaceMedicalRecordDiagnosesRequest.DiagnosisRequest request
    ) {
        return new ReplaceMedicalRecordDiagnosesCommand.DiagnosisCommand(
                request.diagnosisCatalogId(), request.code(), request.name(), request.note()
        );
    }

    private MedicalRecordDiagnosisResponse toResponse(MedicalRecordDiagnosisResult result) {
        return new MedicalRecordDiagnosisResponse(result.id(), result.medicalRecordId(), result.diagnosisCode(),
                result.diagnosisName(), result.diagnosisType(), result.note(), result.diagnosedBy(), result.diagnosedAt());
    }
}
