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
        List<ReplaceMedicalRecordDiagnosesCommand.SecondaryDiagnosisCommand> secondaryDiagnoses = request.secondaryDiagnoses() == null
                ? List.of()
                : request.secondaryDiagnoses().stream().map(this::toSecondaryCommand).toList();
        return new ReplaceMedicalRecordDiagnosesCommand(toPrimaryCommand(request.primaryDiagnosis()), secondaryDiagnoses);
    }

    public List<MedicalRecordDiagnosisResponse> toResponses(List<MedicalRecordDiagnosisResult> results) {
        return results.stream().map(this::toResponse).toList();
    }

    private ReplaceMedicalRecordDiagnosesCommand.PrimaryDiagnosisCommand toPrimaryCommand(
            ReplaceMedicalRecordDiagnosesRequest.PrimaryDiagnosisRequest request
    ) {
        return new ReplaceMedicalRecordDiagnosesCommand.PrimaryDiagnosisCommand(request.diagnosisCatalogId(), request.note());
    }

    private ReplaceMedicalRecordDiagnosesCommand.SecondaryDiagnosisCommand toSecondaryCommand(
            ReplaceMedicalRecordDiagnosesRequest.SecondaryDiagnosisRequest request
    ) {
        return new ReplaceMedicalRecordDiagnosesCommand.SecondaryDiagnosisCommand(
                request.diagnosisCatalogId(), request.name(), request.note()
        );
    }

    private MedicalRecordDiagnosisResponse toResponse(MedicalRecordDiagnosisResult result) {
        return new MedicalRecordDiagnosisResponse(result.id(), result.medicalRecordId(), result.diagnosisCode(),
                result.diagnosisName(), result.diagnosisType(), result.note(), result.diagnosedBy(), result.diagnosedAt());
    }
}
