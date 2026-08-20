package com.benhsoan.domain.clinical.exception;

import com.benhsoan.domain.shared.exception.DomainErrorCode;

import java.util.UUID;

public class ClinicalAttachmentNotFoundException extends ClinicalException {

    public ClinicalAttachmentNotFoundException(UUID attachmentId) {
        super(DomainErrorCode.CLINICAL_ATTACHMENT_NOT_FOUND, "Clinical attachment not found: " + attachmentId);
    }
}
