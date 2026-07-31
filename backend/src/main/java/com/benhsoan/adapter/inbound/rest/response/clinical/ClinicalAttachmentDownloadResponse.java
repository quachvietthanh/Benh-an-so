package com.benhsoan.adapter.inbound.rest.response.clinical;

import java.time.Instant;
import java.util.UUID;

public record ClinicalAttachmentDownloadResponse(UUID attachmentId, String url, Instant expiresAt) {}
