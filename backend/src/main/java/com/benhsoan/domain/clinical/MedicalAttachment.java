package com.benhsoan.domain.clinical;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.clinical.enums.MedicalAttachmentType;
import com.benhsoan.domain.shared.Guard.Guard;
import com.benhsoan.domain.shared.exception.ValidationException;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicalAttachment {

    private UUID id, visitId, medicalRecordId, clinicalResultId, uploadedBy;
    private String fileName, originalFileName, storageKey, contentType, checksum;
    private long fileSize;
    private MedicalAttachmentType attachmentType;
    private Instant uploadedAt;

    private MedicalAttachment(UUID id, UUID visit, UUID record, UUID result, String file, String original, String key, String type, long size, String checksum, MedicalAttachmentType attachmentType, UUID by, Instant at) {
        this.id = Objects.requireNonNull(id);
        visitId = Objects.requireNonNull(visit);
        medicalRecordId = record;
        clinicalResultId = result;
        fileName = Guard.require(file, "File name");
        originalFileName = Guard.require(original, "Original file name");
        storageKey = Guard.require(key, "Storage key");
        contentType = Guard.require(type, "Content type");
        if (size <= 0) {
            throw new ValidationException("File size must be positive.");
        
        }fileSize = size;
        this.checksum = checksum;
        this.attachmentType = Objects.requireNonNull(attachmentType);
        uploadedBy = Objects.requireNonNull(by);
        uploadedAt = Objects.requireNonNull(at);
        validateOwner();
    }

    public static MedicalAttachment create(UUID visit, UUID record, UUID result, String file, String original, String key, String type, long size, String checksum, MedicalAttachmentType attachmentType, UUID by, Instant at) {
        return new MedicalAttachment(UUID.randomUUID(), visit, record, result, file, original, key, type, size, checksum, attachmentType, by, at);
    }

    public static MedicalAttachment restore(UUID id, UUID visit, UUID record, UUID result, String file, String original, String key, String type, long size, String checksum, MedicalAttachmentType attachmentType, UUID by, Instant at) {
        return new MedicalAttachment(id, visit, record, result, file, original, key, type, size, checksum, attachmentType, by, at);
    }

    private void validateOwner() {
        if ((medicalRecordId == null) == (clinicalResultId == null)) {
            throw new ValidationException("Attachment must have exactly one owner.");
        
        }if (attachmentType == MedicalAttachmentType.MEDICAL_RECORD && medicalRecordId == null) 
            throw new ValidationException("Medical record attachment requires medical record.");
        
        if ((attachmentType == MedicalAttachmentType.LAB_RESULT || attachmentType == MedicalAttachmentType.IMAGING_RESULT) && clinicalResultId == null) {
            throw new ValidationException("Result attachment requires clinical result.");
    
        }}
}
