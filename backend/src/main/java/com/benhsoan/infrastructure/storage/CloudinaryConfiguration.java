package com.benhsoan.infrastructure.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Configuration
@ConditionalOnProperty(prefix = "clinical-attachments.cloudinary", name = "enabled", havingValue = "true")
public class CloudinaryConfiguration {

    @Bean
    Cloudinary cloudinary(CloudinaryProperties properties) {
        requireConfigured(properties.cloudName(), "CLOUDINARY_CLOUD_NAME");
        requireConfigured(properties.apiKey(), "CLOUDINARY_API_KEY");
        requireConfigured(properties.apiSecret(), "CLOUDINARY_API_SECRET");
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", properties.cloudName(),
                "api_key", properties.apiKey(),
                "api_secret", properties.apiSecret(),
                "secure", true
        ));
    }

    private void requireConfigured(String value, String environmentVariable) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(environmentVariable + " must be configured when Cloudinary is enabled.");
        }
    }
}
