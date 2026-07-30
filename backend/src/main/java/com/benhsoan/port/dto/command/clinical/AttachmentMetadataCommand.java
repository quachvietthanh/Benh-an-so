package com.benhsoan.port.dto.command.clinical;

import com.benhsoan.domain.shared.exception.ValidationException;

public record AttachmentMetadataCommand(String originalFileName, String contentType, long fileSize) {

    public AttachmentMetadataCommand {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new ValidationException("Attachment file name is required.");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new ValidationException("Attachment content type is required.");
        }
        if (fileSize <= 0) {
            throw new ValidationException("Attachment file size must be positive.");
        }
    }
}
