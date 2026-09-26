package com.benhsoan.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "clinical-attachments")
public record ClinicalAttachmentProperties(
        DataSize maxFileSize,
        List<String> allowedContentTypes
) {
    public ClinicalAttachmentProperties {
        maxFileSize = maxFileSize == null ? DataSize.ofMegabytes(10) : maxFileSize;
        allowedContentTypes = allowedContentTypes == null ? List.of() : List.copyOf(allowedContentTypes);
    }
}
