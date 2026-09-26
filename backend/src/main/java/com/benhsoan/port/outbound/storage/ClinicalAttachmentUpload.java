package com.benhsoan.port.outbound.storage;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public record ClinicalAttachmentUpload(
        UUID clinicalResultId,
        String originalFileName,
        String contentType,
        byte[] content
) {
    public ClinicalAttachmentUpload {
        Objects.requireNonNull(clinicalResultId, "Clinical result id is required.");
        Objects.requireNonNull(originalFileName, "Original file name is required.");
        Objects.requireNonNull(contentType, "Content type is required.");
        content = Arrays.copyOf(Objects.requireNonNull(content, "Content is required."), content.length);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
