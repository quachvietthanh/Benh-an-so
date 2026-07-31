package com.benhsoan.port.dto.result;

import java.time.Instant;
import java.util.UUID;

public record ClinicalAttachmentDownloadResult(UUID attachmentId, String url, Instant expiresAt) {}
