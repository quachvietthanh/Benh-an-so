package com.benhsoan.adapter.inbound.rest.mapper;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.medicalrecord.AmendMedicalRecordRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.CreateMedicalRecordRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.IssueMedicalRecordCopyRequest;
import com.benhsoan.adapter.inbound.rest.request.medicalrecord.UpdateMedicalRecordRequest;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordAccessLogResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordAmendmentResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordClinicalSnapshotResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordVersionHistoryResponse;
import com.benhsoan.adapter.inbound.rest.response.medicalrecord.MedicalRecordVersionResponse;
import com.benhsoan.port.dto.command.medicalrecord.AmendMedicalRecordCommand;
import com.benhsoan.port.dto.command.medicalrecord.CreateMedicalRecordCommand;
import com.benhsoan.port.dto.command.medicalrecord.GetMedicalRecordAccessLogsQuery;
import com.benhsoan.port.dto.command.medicalrecord.IssueMedicalRecordCopyCommand;
import com.benhsoan.port.dto.command.medicalrecord.UpdateMedicalRecordCommand;
import com.benhsoan.port.dto.result.MedicalRecordAccessLogResult;
import com.benhsoan.port.dto.result.MedicalRecordAmendmentResult;
import com.benhsoan.port.dto.result.MedicalRecordClinicalSnapshot;
import com.benhsoan.port.dto.result.MedicalRecordResult;
import com.benhsoan.port.dto.result.MedicalRecordVersion;
import com.benhsoan.port.dto.result.MedicalRecordVersionHistoryResult;

@Component
public class MedicalRecordRestMapper {

    public CreateMedicalRecordCommand toCommand(CreateMedicalRecordRequest request) {
        return new CreateMedicalRecordCommand(request.visitId(), request.chiefComplaint(), request.symptoms(),
                request.medicalHistory(), request.physicalExamination(), request.clinicalProgress(),
                request.treatmentPlan(), request.doctorInstructions(), request.conclusion());
    }

    public UpdateMedicalRecordCommand toCommand(UpdateMedicalRecordRequest request) {
        return new UpdateMedicalRecordCommand(request.chiefComplaint(), request.symptoms(), request.medicalHistory(),
                request.physicalExamination(), request.clinicalProgress(), request.treatmentPlan(),
                request.doctorInstructions(), request.conclusion());
    }

    public AmendMedicalRecordCommand toCommand(AmendMedicalRecordRequest request) {
        return new AmendMedicalRecordCommand(request.content(), request.reason());
    }

    public com.benhsoan.port.dto.command.medicalrecord.SignMedicalRecordCommand toCommand(
            com.benhsoan.adapter.inbound.rest.request.medicalrecord.SignMedicalRecordRequest request
    ) {
        return new com.benhsoan.port.dto.command.medicalrecord.SignMedicalRecordCommand(
                request == null ? null : request.signatureData()
        );
    }

    public IssueMedicalRecordCopyCommand toCommand(UUID medicalRecordId, IssueMedicalRecordCopyRequest request) {
        return new IssueMedicalRecordCopyCommand(medicalRecordId, request.recipientType(),
                request.recipientName(), request.recipientIdentityNumber(),
                request.requestReason(), request.authorizationDocumentNumber());
    }

    public GetMedicalRecordAccessLogsQuery toQuery(
            UUID accessedBy,
            UUID patientId,
            UUID medicalRecordId,
            UUID visitId,
            Instant from,
            Instant to,
            int page,
            int size
    ) {
        return new GetMedicalRecordAccessLogsQuery(accessedBy, patientId, medicalRecordId, visitId, from, to, page, size);
    }

    public MedicalRecordResponse toResponse(MedicalRecordResult result) {
        return new MedicalRecordResponse(result.id(), result.visitId(), result.chiefComplaint(), result.symptoms(),
                result.medicalHistory(), result.physicalExamination(), result.clinicalProgress(), result.treatmentPlan(),
                result.doctorInstructions(), result.conclusion(), result.status(), result.signatureData(),
                result.signedAt(), result.signedBy(), result.lockedAt(), result.lockedBy(),
                result.createdBy(), result.createdAt(), result.updatedBy(), result.updatedAt());
    }

    public MedicalRecordAmendmentResponse toResponse(MedicalRecordAmendmentResult result) {
        return new MedicalRecordAmendmentResponse(result.id(), result.medicalRecordId(), result.content(),
                result.reason(), result.amendedBy(), result.amendedAt());
    }

    public MedicalRecordVersionHistoryResponse toResponse(MedicalRecordVersionHistoryResult result) {
        return new MedicalRecordVersionHistoryResponse(
                result.originalOnly(),
                toResponse(result.originalVersion()),
                result.amendments().stream().map(this::toResponse).toList()
        );
    }

    private MedicalRecordVersionResponse toResponse(MedicalRecordVersion version) {
        return new MedicalRecordVersionResponse(
                version.versionNumber(), version.modifiedBy(), version.modifiedAt(),
                version.reason(), version.content(), toResponse(version.snapshot())
        );
    }

    private MedicalRecordClinicalSnapshotResponse toResponse(MedicalRecordClinicalSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return new MedicalRecordClinicalSnapshotResponse(
                snapshot.chiefComplaint(), snapshot.symptoms(), snapshot.medicalHistory(),
                snapshot.physicalExamination(), snapshot.clinicalProgress(), snapshot.treatmentPlan(),
                snapshot.doctorInstructions(), snapshot.conclusion(), snapshot.diagnoses()
        );
    }

    public Page<MedicalRecordAccessLogResponse> toAccessLogResponse(Page<MedicalRecordAccessLogResult> resultPage) {
        return resultPage.map(result -> new MedicalRecordAccessLogResponse(result.id(), result.patientId(),
                result.visitId(), result.medicalRecordId(), result.accessedBy(), result.action(),
                result.detail(), result.accessedAt()));
    }
}
