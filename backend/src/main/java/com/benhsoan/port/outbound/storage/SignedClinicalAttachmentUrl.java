package com.benhsoan.port.outbound.storage;

import java.time.Instant;
import java.util.Objects;

public record SignedClinicalAttachmentUrl(String url, Instant expiresAt) {
    public SignedClinicalAttachmentUrl {
        Objects.requireNonNull(url, "URL is required.");
        Objects.requireNonNull(expiresAt, "Expiry time is required.");
    }
}
