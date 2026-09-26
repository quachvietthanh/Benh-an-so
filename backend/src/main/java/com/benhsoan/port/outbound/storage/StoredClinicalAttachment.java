package com.benhsoan.port.outbound.storage;

import java.util.Objects;

public record StoredClinicalAttachment(
        String publicId,
        ClinicalAttachmentResourceType resourceType,
        String secureUrl,
        long fileSize
) {
    public StoredClinicalAttachment {
        Objects.requireNonNull(publicId, "Public id is required.");
        Objects.requireNonNull(resourceType, "Resource type is required.");
        Objects.requireNonNull(secureUrl, "Secure URL is required.");
        if (fileSize <= 0) {
            throw new IllegalArgumentException("File size must be positive.");
        }
    }
}
