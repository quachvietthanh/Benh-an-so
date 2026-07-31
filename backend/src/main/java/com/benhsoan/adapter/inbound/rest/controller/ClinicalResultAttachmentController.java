package com.benhsoan.adapter.inbound.rest.controller;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.benhsoan.adapter.inbound.rest.mapper.ClinicalResultAttachmentRestMapper;
import com.benhsoan.adapter.inbound.rest.response.clinical.ClinicalAttachmentDownloadResponse;
import com.benhsoan.adapter.inbound.rest.response.clinical.ClinicalResultResponse;
import com.benhsoan.port.inbound.clinical.DownloadClinicalResultAttachmentUseCase;
import com.benhsoan.port.inbound.clinical.UploadClinicalResultAttachmentUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "clinical-attachments.cloudinary", name = "enabled", havingValue = "true")
public class ClinicalResultAttachmentController {

    private final UploadClinicalResultAttachmentUseCase uploadClinicalResultAttachmentUseCase;
    private final DownloadClinicalResultAttachmentUseCase downloadClinicalResultAttachmentUseCase;
    private final ClinicalResultAttachmentRestMapper mapper;

    @PostMapping(value = "/clinical-results/{resultId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ClinicalResultResponse.AttachmentResponse upload(@PathVariable UUID resultId,
            @RequestPart("file") MultipartFile file) {
        return mapper.toResponse(uploadClinicalResultAttachmentUseCase.upload(resultId, mapper.toCommand(file)));
    }

    @GetMapping("/clinical-result-attachments/{attachmentId}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'NURSE')")
    public ClinicalAttachmentDownloadResponse download(@PathVariable UUID attachmentId) {
        return mapper.toResponse(downloadClinicalResultAttachmentUseCase.createDownloadUrl(attachmentId));
    }
}
