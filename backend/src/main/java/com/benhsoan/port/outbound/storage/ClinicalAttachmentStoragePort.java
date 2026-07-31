package com.benhsoan.port.outbound.storage;

public interface ClinicalAttachmentStoragePort {

    StoredClinicalAttachment upload(ClinicalAttachmentUpload upload);

    void delete(String publicId, ClinicalAttachmentResourceType resourceType);

    SignedClinicalAttachmentUrl generateSignedDownloadUrl(String publicId,
            ClinicalAttachmentResourceType resourceType);
}
