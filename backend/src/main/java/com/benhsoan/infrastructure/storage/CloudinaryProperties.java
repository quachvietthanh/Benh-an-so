package com.benhsoan.infrastructure.storage;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clinical-attachments.cloudinary")
public record CloudinaryProperties(
        boolean enabled,
        String cloudName,
        String apiKey,
        String apiSecret,
        String folderPrefix,
        Duration signedUrlTtl
) {}
