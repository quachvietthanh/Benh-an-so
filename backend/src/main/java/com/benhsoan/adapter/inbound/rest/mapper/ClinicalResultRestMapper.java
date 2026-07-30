package com.benhsoan.adapter.inbound.rest.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.clinical.AttachmentMetadataRequest;
import com.benhsoan.adapter.inbound.rest.request.clinical.EnterClinicalResultRequest;
import com.benhsoan.adapter.inbound.rest.request.clinical.UpdateClinicalResultRequest;
import com.benhsoan.adapter.inbound.rest.response.clinical.ClinicalResultResponse;
import com.benhsoan.port.dto.command.clinical.AttachmentMetadataCommand;
import com.benhsoan.port.dto.command.clinical.EnterClinicalResultCommand;
import com.benhsoan.port.dto.command.clinical.UpdateClinicalResultCommand;
import com.benhsoan.port.dto.result.ClinicalResultResult;

@Component
public class ClinicalResultRestMapper {

    public EnterClinicalResultCommand toCommand(EnterClinicalResultRequest request) {
        return new EnterClinicalResultCommand(request.numericValue(), request.textValue(), request.abnormalFlag(),
                request.conclusion(), toAttachmentCommands(request.attachments()));
    }

    public UpdateClinicalResultCommand toCommand(UpdateClinicalResultRequest request) {
        return new UpdateClinicalResultCommand(request.numericValue(), request.textValue(), request.abnormalFlag(),
                request.conclusion(), request.changeReason(), toAttachmentCommands(request.attachments()));
    }

    public ClinicalResultResponse toResponse(ClinicalResultResult result) {
        return new ClinicalResultResponse(result.id(), result.clinicalOrderItemId(), result.visitId(),
                result.resultType(), result.numericValue(), result.textValue(), result.unit(), result.referenceRange(),
                result.abnormalFlag(), result.conclusion(), result.status(),
                result.attachments().stream().map(this::toAttachmentResponse).toList(),
                result.histories().stream().map(this::toHistoryResponse).toList());
    }

    public ClinicalResultResponse.HistoryResponse toHistoryResponse(ClinicalResultResult.History history) {
        return new ClinicalResultResponse.HistoryResponse(history.id(), history.oldResultType(), history.newResultType(),
                history.oldNumericValue(), history.newNumericValue(), history.oldTextValue(), history.newTextValue(),
                history.oldUnit(), history.newUnit(), history.oldReferenceRange(), history.newReferenceRange(),
                history.oldAbnormalFlag(), history.newAbnormalFlag(), history.oldConclusion(), history.newConclusion(),
                history.oldStatus(), history.newStatus(), history.changeReason(), history.changedBy(), history.changedAt());
    }

    private List<AttachmentMetadataCommand> toAttachmentCommands(List<AttachmentMetadataRequest> attachments) {
        if (attachments == null) {
            return List.of();
        }
        return attachments.stream().map(attachment -> new AttachmentMetadataCommand(attachment.originalFileName(),
                attachment.contentType(), attachment.fileSize())).toList();
    }

    private ClinicalResultResponse.AttachmentResponse toAttachmentResponse(ClinicalResultResult.Attachment attachment) {
        return new ClinicalResultResponse.AttachmentResponse(attachment.id(), attachment.fileName(),
                attachment.contentType(), attachment.fileSize(), attachment.attachmentType());
    }
}
