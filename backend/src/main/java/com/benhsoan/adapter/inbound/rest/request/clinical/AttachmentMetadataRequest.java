package com.benhsoan.adapter.inbound.rest.request.clinical;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AttachmentMetadataRequest(
        @NotBlank String originalFileName,
        @NotBlank String contentType,
        @Positive long fileSize
) {}
