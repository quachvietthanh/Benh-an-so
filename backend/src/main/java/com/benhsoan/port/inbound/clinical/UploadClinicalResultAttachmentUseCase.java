package com.benhsoan.port.inbound.clinical;

import java.util.UUID;

import com.benhsoan.port.dto.command.clinical.UploadClinicalResultAttachmentCommand;
import com.benhsoan.port.dto.result.ClinicalResultResult;

public interface UploadClinicalResultAttachmentUseCase {

    ClinicalResultResult.Attachment upload(UUID clinicalResultId, UploadClinicalResultAttachmentCommand command);
}
