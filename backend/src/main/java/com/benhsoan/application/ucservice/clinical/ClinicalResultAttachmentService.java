package com.benhsoan.application.ucservice.clinical;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.benhsoan.domain.clinical.MedicalAttachment;
import com.benhsoan.domain.clinical.enums.ClinicalResultStatus;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;
import com.benhsoan.domain.clinical.enums.MedicalAttachmentType;
import com.benhsoan.domain.clinical.exception.ClinicalOrderInvalidVisitException;
import com.benhsoan.domain.clinical.exception.ClinicalOrderLockedMedicalRecordException;
import com.benhsoan.domain.clinical.exception.ClinicalResultAlreadyFinalizedException;
import com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.clinical.UploadClinicalResultAttachmentCommand;
import com.benhsoan.port.dto.result.ClinicalAttachmentDownloadResult;
import com.benhsoan.port.dto.result.ClinicalResultResult;
import com.benhsoan.port.inbound.clinical.DownloadClinicalResultAttachmentUseCase;
import com.benhsoan.port.inbound.clinical.UploadClinicalResultAttachmentUseCase;
import com.benhsoan.port.outbound.repository.crudRepository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.crudRepository.clinical.ClinicalResultRepository;
import com.benhsoan.port.outbound.repository.crudRepository.clinical.ClinicalServiceCatalogRepository;
import com.benhsoan.port.outbound.repository.crudRepository.clinical.MedicalAttachmentRepository;
import com.benhsoan.port.outbound.repository.crudRepository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.crudRepository.visit.VisitRepository;
import com.benhsoan.port.outbound.storage.ClinicalAttachmentResourceType;
import com.benhsoan.port.outbound.storage.ClinicalAttachmentStoragePort;
import com.benhsoan.port.outbound.storage.ClinicalAttachmentUpload;
import com.benhsoan.port.outbound.storage.StoredClinicalAttachment;
import com.benhsoan.port.outbound.time.ClockPort;

@Service
@Transactional
@ConditionalOnProperty(prefix = "clinical-attachments.cloudinary", name = "enabled", havingValue = "true")
public class ClinicalResultAttachmentService implements UploadClinicalResultAttachmentUseCase,
        DownloadClinicalResultAttachmentUseCase {

    private static final Logger log = LoggerFactory.getLogger(ClinicalResultAttachmentService.class);

    private final ClinicalResultRepository clinicalResultRepository;
    private final ClinicalOrderItemRepository clinicalOrderItemRepository;
    private final ClinicalServiceCatalogRepository clinicalServiceCatalogRepository;
    private final MedicalAttachmentRepository medicalAttachmentRepository;
    private final VisitRepository visitRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final ClinicalOrderAuthorizationService authorizationService;
    private final ClinicalResultAuditService auditService;
    private final ClinicalAttachmentValidator validator;
    private final ClinicalAttachmentStoragePort storagePort;
    private final ClockPort clock;

    public ClinicalResultAttachmentService(ClinicalResultRepository clinicalResultRepository,
            ClinicalOrderItemRepository clinicalOrderItemRepository,
            ClinicalServiceCatalogRepository clinicalServiceCatalogRepository,
            MedicalAttachmentRepository medicalAttachmentRepository, VisitRepository visitRepository,
            MedicalRecordRepository medicalRecordRepository, ClinicalOrderAuthorizationService authorizationService,
            ClinicalResultAuditService auditService, ClinicalAttachmentValidator validator,
            ClinicalAttachmentStoragePort storagePort, ClockPort clock) {
        this.clinicalResultRepository = clinicalResultRepository;
        this.clinicalOrderItemRepository = clinicalOrderItemRepository;
        this.clinicalServiceCatalogRepository = clinicalServiceCatalogRepository;
        this.medicalAttachmentRepository = medicalAttachmentRepository;
        this.visitRepository = visitRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.validator = validator;
        this.storagePort = storagePort;
        this.clock = clock;
    }

    @Override
    public ClinicalResultResult.Attachment upload(UUID clinicalResultId, UploadClinicalResultAttachmentCommand command) {
        UUID actorId = authorizationService.requireWriteAccess();
        validator.validate(command);
        String contentType = command.contentType().toLowerCase(Locale.ROOT);

        var result = clinicalResultRepository.findById(clinicalResultId)
                .orElseThrow(() -> new ValidationException("Clinical result not found."));
        if (result.getStatus() == ClinicalResultStatus.FINAL) {
            throw new ClinicalResultAlreadyFinalizedException();
        }
        var visit = requireWritableVisitAndRecord(result.getVisitId());
        var item = clinicalOrderItemRepository.findById(result.getClinicalOrderItemId())
                .orElseThrow(() -> new ValidationException("Clinical order item not found."));
        var clinicalService = clinicalServiceCatalogRepository.findById(item.getClinicalServiceId())
                .orElseThrow(() -> new ValidationException("Clinical service not found."));

        StoredClinicalAttachment stored = storagePort.upload(new ClinicalAttachmentUpload(clinicalResultId,
                command.originalFileName(), contentType, command.content()));
        registerRollbackCompensation(stored);

        Instant now = clock.now();
        MedicalAttachment attachment = medicalAttachmentRepository.save(MedicalAttachment.create(
                visit.getId(), null, clinicalResultId, command.originalFileName(), command.originalFileName(),
                stored.publicId(), contentType, stored.fileSize(), checksum(command.content()),
                attachmentTypeFor(clinicalService.getServiceType()), actorId, now));
        auditService.recordWrite(clinicalResultId, visit.getPatientId(), visit.getId(),
                medicalRecordRepository.findByVisitId(visit.getId()).orElseThrow(
                        () -> new ValidationException("Medical record not found.")).getId(),
                actorId, MedicalRecordAccessAction.UPDATE, now);
        return new ClinicalResultResult.Attachment(attachment.getId(), attachment.getFileName(),
                attachment.getContentType(), attachment.getFileSize(), attachment.getAttachmentType());
    }

    @Override
    @Transactional(readOnly = true)
    public ClinicalAttachmentDownloadResult createDownloadUrl(UUID attachmentId) {
        UUID actorId = authorizationService.requireReadAccess();
        MedicalAttachment attachment = medicalAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ValidationException("Clinical attachment not found."));
        if (attachment.getClinicalResultId() == null) {
            throw new ValidationException("Clinical attachment does not belong to a clinical result.");
        }
        var result = clinicalResultRepository.findById(attachment.getClinicalResultId())
                .orElseThrow(() -> new ValidationException("Clinical result not found."));
        var visit = visitRepository.findById(result.getVisitId())
                .orElseThrow(() -> new ValidationException("Visit not found."));
        var medicalRecord = medicalRecordRepository.findByVisitId(visit.getId())
                .orElseThrow(() -> new ValidationException("Medical record not found."));

        var signedUrl = storagePort.generateSignedDownloadUrl(attachment.getStorageKey(),
                resourceTypeFor(attachment.getContentType()));
        auditService.recordView(result.getId(), visit.getPatientId(), visit.getId(), medicalRecord.getId(), actorId,
                MedicalRecordAccessAction.VIEW, clock.now());
        return new ClinicalAttachmentDownloadResult(attachment.getId(), signedUrl.url(), signedUrl.expiresAt());
    }

    private com.benhsoan.domain.visit.Visit requireWritableVisitAndRecord(UUID visitId) {
        var visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ValidationException("Visit not found."));
        if (!visit.isActive()) {
            throw new ClinicalOrderInvalidVisitException();
        }
        var medicalRecord = medicalRecordRepository.findByVisitId(visitId)
                .orElseThrow(() -> new ValidationException("Medical record not found."));
        if (medicalRecord.isLocked()) {
            throw new ClinicalOrderLockedMedicalRecordException();
        }
        return visit;
    }

    private void registerRollbackCompensation(StoredClinicalAttachment stored) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    return;
                }
                try {
                    storagePort.delete(stored.publicId(), stored.resourceType());
                } catch (RuntimeException ex) {
                    log.error("Failed to compensate Cloudinary attachment upload after transaction rollback.", ex);
                }
            }
        });
    }

    private MedicalAttachmentType attachmentTypeFor(ClinicalServiceType serviceType) {
        return switch (serviceType) {
            case IMAGING -> MedicalAttachmentType.IMAGING_RESULT;
            case LAB_TEST -> MedicalAttachmentType.LAB_RESULT;
            default -> MedicalAttachmentType.OTHER;
        };
    }

    private ClinicalAttachmentResourceType resourceTypeFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg", "image/png" -> ClinicalAttachmentResourceType.IMAGE;
            case "application/pdf" -> ClinicalAttachmentResourceType.RAW;
            default -> throw new ValidationException("Clinical attachment content type is not supported.");
        };
    }

    private String checksum(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable.", ex);
        }
    }
}
