package com.benhsoan.adapter.inbound.rest.mapper;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.benhsoan.adapter.inbound.rest.response.clinical.ClinicalAttachmentDownloadResponse;
import com.benhsoan.adapter.inbound.rest.response.clinical.ClinicalResultResponse;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.clinical.UploadClinicalResultAttachmentCommand;
import com.benhsoan.port.dto.result.ClinicalAttachmentDownloadResult;
import com.benhsoan.port.dto.result.ClinicalResultResult;

@Component
public class ClinicalResultAttachmentRestMapper {

    public UploadClinicalResultAttachmentCommand toCommand(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Clinical attachment file is required.");
        }
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        if (!StringUtils.hasText(fileName)) {
            throw new ValidationException("Clinical attachment file name is required.");
        }
        if (!StringUtils.hasText(contentType)) {
            throw new ValidationException("Clinical attachment content type is required.");
        }
        try {
            return new UploadClinicalResultAttachmentCommand(fileName, contentType, file.getBytes());
        } catch (IOException ex) {
            throw new ValidationException("Clinical attachment file cannot be read.");
        }
    }

    public ClinicalResultResponse.AttachmentResponse toResponse(ClinicalResultResult.Attachment attachment) {
        return new ClinicalResultResponse.AttachmentResponse(attachment.id(), attachment.fileName(),
                attachment.contentType(), attachment.fileSize(), attachment.attachmentType());
    }

    public ClinicalAttachmentDownloadResponse toResponse(ClinicalAttachmentDownloadResult result) {
        return new ClinicalAttachmentDownloadResponse(result.attachmentId(), result.url(), result.expiresAt());
    }
}
