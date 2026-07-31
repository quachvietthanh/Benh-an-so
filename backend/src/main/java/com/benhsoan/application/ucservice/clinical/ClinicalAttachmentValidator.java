package com.benhsoan.application.ucservice.clinical;

import java.util.Locale;

import org.springframework.stereotype.Component;

import com.benhsoan.config.ClinicalAttachmentProperties;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.clinical.UploadClinicalResultAttachmentCommand;

@Component
public class ClinicalAttachmentValidator {

    private final ClinicalAttachmentProperties properties;

    public ClinicalAttachmentValidator(ClinicalAttachmentProperties properties) {
        this.properties = properties;
    }

    public void validate(UploadClinicalResultAttachmentCommand command) {
        String contentType = command.contentType().toLowerCase(Locale.ROOT);
        byte[] content = command.content();
        if (!properties.allowedContentTypes().contains(contentType)) {
            throw new ValidationException("Clinical attachment content type is not allowed.");
        }
        if (content.length == 0 || content.length > properties.maxFileSize().toBytes()) {
            throw new ValidationException("Clinical attachment file size is invalid.");
        }
        if (!hasExpectedExtension(command.originalFileName(), contentType)) {
            throw new ValidationException("Clinical attachment file extension does not match its content type.");
        }
        if (!hasExpectedSignature(content, contentType)) {
            throw new ValidationException("Clinical attachment file signature is invalid.");
        }
    }

    private boolean hasExpectedExtension(String fileName, String contentType) {
        String normalizedFileName = fileName.toLowerCase(Locale.ROOT);
        return switch (contentType) {
            case "image/jpeg" -> normalizedFileName.endsWith(".jpg") || normalizedFileName.endsWith(".jpeg");
            case "image/png" -> normalizedFileName.endsWith(".png");
            case "application/pdf" -> normalizedFileName.endsWith(".pdf");
            default -> false;
        };
    }

    private boolean hasExpectedSignature(byte[] content, String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> content.length >= 3
                    && content[0] == (byte) 0xFF && content[1] == (byte) 0xD8 && content[2] == (byte) 0xFF;
            case "image/png" -> content.length >= 8
                    && content[0] == (byte) 0x89 && content[1] == 0x50 && content[2] == 0x4E && content[3] == 0x47
                    && content[4] == 0x0D && content[5] == 0x0A && content[6] == 0x1A && content[7] == 0x0A;
            case "application/pdf" -> content.length >= 5
                    && content[0] == '%' && content[1] == 'P' && content[2] == 'D' && content[3] == 'F' && content[4] == '-';
            default -> false;
        };
    }
}
