package com.benhsoan.adapter.inbound.rest.mapper;

import org.springframework.stereotype.Component;

import com.benhsoan.adapter.inbound.rest.request.clinical.EnterClinicalResultRequest;
import com.benhsoan.adapter.inbound.rest.request.clinical.UpdateClinicalResultRequest;
import com.benhsoan.adapter.inbound.rest.response.clinical.ClinicalResultResponse;
import com.benhsoan.port.dto.command.clinical.EnterClinicalResultCommand;
import com.benhsoan.port.dto.command.clinical.UpdateClinicalResultCommand;
import com.benhsoan.port.dto.result.ClinicalResultResult;

@Component
public class ClinicalResultRestMapper {

    public EnterClinicalResultCommand toCommand(EnterClinicalResultRequest request) {
        return new EnterClinicalResultCommand(request.numericValue(), request.textValue(), request.abnormalFlag(),
                request.conclusion());
    }

    public UpdateClinicalResultCommand toCommand(UpdateClinicalResultRequest request) {
        return new UpdateClinicalResultCommand(request.numericValue(), request.textValue(), request.abnormalFlag(),
                request.conclusion(), request.changeReason());
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

    private ClinicalResultResponse.AttachmentResponse toAttachmentResponse(ClinicalResultResult.Attachment attachment) {
        return new ClinicalResultResponse.AttachmentResponse(attachment.id(), attachment.fileName(),
                attachment.contentType(), attachment.fileSize(), attachment.attachmentType());
    }
}
