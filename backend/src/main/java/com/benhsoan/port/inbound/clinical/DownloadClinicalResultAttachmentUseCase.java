package com.benhsoan.port.inbound.clinical;

import java.util.UUID;

import com.benhsoan.port.dto.result.ClinicalAttachmentDownloadResult;

public interface DownloadClinicalResultAttachmentUseCase {

    ClinicalAttachmentDownloadResult createDownloadUrl(UUID attachmentId);
}
