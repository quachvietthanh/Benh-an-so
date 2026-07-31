package com.benhsoan.infrastructure.storage;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.benhsoan.port.outbound.storage.ClinicalAttachmentResourceType;
import com.benhsoan.port.outbound.storage.ClinicalAttachmentStoragePort;
import com.benhsoan.port.outbound.storage.ClinicalAttachmentUpload;
import com.benhsoan.port.outbound.storage.SignedClinicalAttachmentUrl;
import com.benhsoan.port.outbound.storage.StoredClinicalAttachment;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Component
@ConditionalOnProperty(prefix = "clinical-attachments.cloudinary", name = "enabled", havingValue = "true")
public class CloudinaryClinicalAttachmentStorageAdapter implements ClinicalAttachmentStoragePort {

    private static final String AUTHENTICATED_DELIVERY_TYPE = "authenticated";

    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;

    public CloudinaryClinicalAttachmentStorageAdapter(Cloudinary cloudinary, CloudinaryProperties properties) {
        this.cloudinary = cloudinary;
        this.properties = properties;
    }

    @Override
    public StoredClinicalAttachment upload(ClinicalAttachmentUpload upload) {
        ClinicalAttachmentResourceType resourceType = resourceTypeFor(upload.contentType());
        String publicId = createPublicId(upload.clinicalResultId());
        try {
            Map<?, ?> response = cloudinary.uploader().upload(upload.content(), ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", toCloudinaryResourceType(resourceType),
                    "type", AUTHENTICATED_DELIVERY_TYPE,
                    "overwrite", false,
                    "use_filename", false,
                    "unique_filename", false
            ));
            return new StoredClinicalAttachment(
                    requiredString(response, "public_id"),
                    resourceType,
                    requiredString(response, "secure_url"),
                    requiredFileSize(response)
            );
        } catch (Exception ex) {
            throw new CloudinaryAttachmentStorageException("Unable to upload clinical attachment.", ex);
        }
    }

    @Override
    public void delete(String publicId, ClinicalAttachmentResourceType resourceType) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", toCloudinaryResourceType(resourceType),
                    "type", AUTHENTICATED_DELIVERY_TYPE,
                    "invalidate", true
            ));
        } catch (Exception ex) {
            throw new CloudinaryAttachmentStorageException("Unable to delete clinical attachment.", ex);
        }
    }

    @Override
    public SignedClinicalAttachmentUrl generateSignedDownloadUrl(String publicId,
            ClinicalAttachmentResourceType resourceType) {
        Instant expiresAt = Instant.now().plus(properties.signedUrlTtl());
        try {
            String url = cloudinary.privateDownload(publicId, null, ObjectUtils.asMap(
                    "resource_type", toCloudinaryResourceType(resourceType),
                    "type", AUTHENTICATED_DELIVERY_TYPE,
                    "expires_at", expiresAt.getEpochSecond()
            ));
            return new SignedClinicalAttachmentUrl(url, expiresAt);
        } catch (Exception ex) {
            throw new CloudinaryAttachmentStorageException("Unable to generate clinical attachment download URL.", ex);
        }
    }

    private ClinicalAttachmentResourceType resourceTypeFor(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg", "image/png" -> ClinicalAttachmentResourceType.IMAGE;
            case "application/pdf" -> ClinicalAttachmentResourceType.RAW;
            default -> throw new IllegalArgumentException("Unsupported clinical attachment content type.");
        };
    }

    private String createPublicId(UUID clinicalResultId) {
        String prefix = properties.folderPrefix();
        if (!StringUtils.hasText(prefix)) {
            prefix = "benh-soan/clinical-results";
        }
        String normalizedPrefix = prefix.replaceAll("^/+|/+$", "");
        return normalizedPrefix + "/" + clinicalResultId + "/" + UUID.randomUUID();
    }

    private String toCloudinaryResourceType(ClinicalAttachmentResourceType resourceType) {
        return resourceType.name().toLowerCase(Locale.ROOT);
    }

    private String requiredString(Map<?, ?> response, String key) {
        Object value = response.get(key);
        if (!(value instanceof String stringValue) || !StringUtils.hasText(stringValue)) {
            throw new IllegalStateException("Cloudinary upload response is missing " + key + ".");
        }
        return stringValue;
    }

    private long requiredFileSize(Map<?, ?> response) {
        Object value = response.get("bytes");
        if (!(value instanceof Number number) || number.longValue() <= 0) {
            throw new IllegalStateException("Cloudinary upload response is missing file size.");
        }
        return number.longValue();
    }
}
