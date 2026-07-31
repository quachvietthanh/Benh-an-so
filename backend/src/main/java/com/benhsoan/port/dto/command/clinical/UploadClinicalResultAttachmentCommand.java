package com.benhsoan.port.dto.command.clinical;

import java.util.Arrays;
import java.util.Objects;

public record UploadClinicalResultAttachmentCommand(
        String originalFileName,
        String contentType,
        byte[] content
) {
    public UploadClinicalResultAttachmentCommand {
        Objects.requireNonNull(originalFileName, "Original file name is required.");
        Objects.requireNonNull(contentType, "Content type is required.");
        content = Arrays.copyOf(Objects.requireNonNull(content, "Content is required."), content.length);
    }

    @Override
    public byte[] content() {
        return Arrays.copyOf(content, content.length);
    }
}
