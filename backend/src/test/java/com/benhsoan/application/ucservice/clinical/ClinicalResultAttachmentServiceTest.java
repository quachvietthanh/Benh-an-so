package com.benhsoan.application.ucservice.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.unit.DataSize;

import com.benhsoan.config.ClinicalAttachmentProperties;
import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.domain.clinical.ClinicalResult;
import com.benhsoan.domain.clinical.ClinicalServiceCatalog;
import com.benhsoan.domain.clinical.MedicalAttachment;
import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;
import com.benhsoan.domain.clinical.enums.ClinicalResultType;
import com.benhsoan.domain.clinical.enums.ClinicalResultStatus;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;
import com.benhsoan.domain.clinical.enums.MedicalAttachmentType;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.command.clinical.UploadClinicalResultAttachmentCommand;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalResultRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalServiceCatalogRepository;
import com.benhsoan.port.outbound.repository.clinical.MedicalAttachmentRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.storage.ClinicalAttachmentResourceType;
import com.benhsoan.port.outbound.storage.ClinicalAttachmentStoragePort;
import com.benhsoan.port.outbound.storage.SignedClinicalAttachmentUrl;
import com.benhsoan.port.outbound.storage.StoredClinicalAttachment;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class ClinicalResultAttachmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T01:00:00Z");

    @Mock private ClinicalResultRepository clinicalResultRepository;
    @Mock private ClinicalOrderItemRepository clinicalOrderItemRepository;
    @Mock private ClinicalServiceCatalogRepository clinicalServiceCatalogRepository;
    @Mock private MedicalAttachmentRepository medicalAttachmentRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private ClinicalOrderAuthorizationService authorizationService;
    @Mock private ClinicalResultAuditService auditService;
    @Mock private ClinicalAttachmentStoragePort storagePort;
    @Mock private ClockPort clock;

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void uploadsPdfStoresCloudinaryPublicIdAndDeletesItOnRollback() {
        Fixture fixture = fixture();
        ClinicalResultAttachmentService service = service();
        StoredClinicalAttachment stored = new StoredClinicalAttachment("benh-soan/clinical-results/result/file",
                ClinicalAttachmentResourceType.RAW, "https://cloudinary.example/file", 13);
        when(authorizationService.requireWriteAccess()).thenReturn(fixture.actorId());
        when(clinicalResultRepository.findById(fixture.result().getId())).thenReturn(Optional.of(fixture.result()));
        when(visitRepository.findById(fixture.visit().getId())).thenReturn(Optional.of(fixture.visit()));
        when(medicalRecordRepository.findByVisitId(fixture.visit().getId())).thenReturn(Optional.of(fixture.record()));
        when(clinicalOrderItemRepository.findById(fixture.item().getId())).thenReturn(Optional.of(fixture.item()));
        when(clinicalServiceCatalogRepository.findById(fixture.item().getClinicalServiceId()))
                .thenReturn(Optional.of(fixture.service()));
        when(storagePort.upload(any())).thenReturn(stored);
        when(clock.now()).thenReturn(NOW);
        when(medicalAttachmentRepository.save(any(MedicalAttachment.class))).thenAnswer(call -> call.getArgument(0));
        TransactionSynchronizationManager.initSynchronization();

        var response = service.upload(fixture.result().getId(), pdfCommand());

        ArgumentCaptor<MedicalAttachment> attachmentCaptor = ArgumentCaptor.forClass(MedicalAttachment.class);
        verify(medicalAttachmentRepository).save(attachmentCaptor.capture());
        assertEquals(stored.publicId(), attachmentCaptor.getValue().getStorageKey());
        assertEquals(64, attachmentCaptor.getValue().getChecksum().length());
        assertEquals(MedicalAttachmentType.LAB_RESULT, response.attachmentType());
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(storagePort).delete(stored.publicId(), ClinicalAttachmentResourceType.RAW);
    }

    @Test
    void createsSignedUrlForAuthorizedReader() {
        Fixture fixture = fixture();
        ClinicalResultAttachmentService service = service();
        MedicalAttachment attachment = MedicalAttachment.create(fixture.visit().getId(), null, fixture.result().getId(),
                "result.pdf", "result.pdf", "benh-soan/clinical-results/result/file", "application/pdf", 13,
                "checksum", MedicalAttachmentType.LAB_RESULT, fixture.actorId(), NOW);
        Instant expiresAt = NOW.plusSeconds(300);
        when(authorizationService.requireReadAccess()).thenReturn(fixture.actorId());
        when(medicalAttachmentRepository.findById(attachment.getId())).thenReturn(Optional.of(attachment));
        when(clinicalResultRepository.findById(fixture.result().getId())).thenReturn(Optional.of(fixture.result()));
        when(visitRepository.findById(fixture.visit().getId())).thenReturn(Optional.of(fixture.visit()));
        when(medicalRecordRepository.findByVisitId(fixture.visit().getId())).thenReturn(Optional.of(fixture.record()));
        when(storagePort.generateSignedDownloadUrl(attachment.getStorageKey(), ClinicalAttachmentResourceType.RAW))
                .thenReturn(new SignedClinicalAttachmentUrl("https://cloudinary.example/signed", expiresAt));
        when(clock.now()).thenReturn(NOW);

        var response = service.createDownloadUrl(attachment.getId());

        assertEquals("https://cloudinary.example/signed", response.url());
        assertEquals(expiresAt, response.expiresAt());
        verify(auditService).recordView(fixture.result().getId(), fixture.visit().getPatientId(), fixture.visit().getId(),
                fixture.record().getId(), fixture.actorId(), com.benhsoan.domain.medicalrecord.enums.MedicalRecordAccessAction.VIEW, NOW);
    }

    @Test
    void rejectsFileWithMismatchedMagicBytes() {
        ClinicalAttachmentValidator validator = new ClinicalAttachmentValidator(properties());

        assertThrows(com.benhsoan.domain.shared.exception.ValidationException.class,
                () -> validator.validate(new UploadClinicalResultAttachmentCommand("result.pdf", "application/pdf",
                        "not a pdf".getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    }

    @Test
    void rejectsUploadForFinalizedResultBeforeCallingStorage() {
        Fixture fixture = fixture();
        ClinicalResultAttachmentService service = service();
        ClinicalResult finalized = ClinicalResult.restore(fixture.result().getId(), fixture.item().getId(),
                fixture.visit().getId(), ClinicalResultType.FILE, null, null, null, null,
                ClinicalResultAbnormalFlag.UNKNOWN, null, ClinicalResultStatus.FINAL, fixture.actorId(), NOW,
                fixture.actorId(), NOW);
        when(authorizationService.requireWriteAccess()).thenReturn(fixture.actorId());
        when(clinicalResultRepository.findById(finalized.getId())).thenReturn(Optional.of(finalized));

        assertThrows(com.benhsoan.domain.clinical.exception.ClinicalResultAlreadyFinalizedException.class,
                () -> service.upload(finalized.getId(), pdfCommand()));

        verifyNoInteractions(storagePort, medicalAttachmentRepository);
    }

    @Test
    void normalizesUploadedContentTypeBeforePersisting() {
        Fixture fixture = fixture();
        ClinicalResultAttachmentService service = service();
        StoredClinicalAttachment stored = new StoredClinicalAttachment("clinical/result", ClinicalAttachmentResourceType.RAW,
                "https://cloudinary.example/file", 13);
        when(authorizationService.requireWriteAccess()).thenReturn(fixture.actorId());
        when(clinicalResultRepository.findById(fixture.result().getId())).thenReturn(Optional.of(fixture.result()));
        when(visitRepository.findById(fixture.visit().getId())).thenReturn(Optional.of(fixture.visit()));
        when(medicalRecordRepository.findByVisitId(fixture.visit().getId())).thenReturn(Optional.of(fixture.record()));
        when(clinicalOrderItemRepository.findById(fixture.item().getId())).thenReturn(Optional.of(fixture.item()));
        when(clinicalServiceCatalogRepository.findById(fixture.item().getClinicalServiceId()))
                .thenReturn(Optional.of(fixture.service()));
        when(storagePort.upload(any())).thenReturn(stored);
        when(medicalAttachmentRepository.save(any(MedicalAttachment.class))).thenAnswer(call -> call.getArgument(0));
        when(clock.now()).thenReturn(NOW);

        service.upload(fixture.result().getId(), new UploadClinicalResultAttachmentCommand("result.pdf",
                "APPLICATION/PDF", "%PDF-1.7\nbody".getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        ArgumentCaptor<MedicalAttachment> attachmentCaptor = ArgumentCaptor.forClass(MedicalAttachment.class);
        verify(medicalAttachmentRepository).save(attachmentCaptor.capture());
        assertEquals("application/pdf", attachmentCaptor.getValue().getContentType());
    }

    private ClinicalResultAttachmentService service() {
        return new ClinicalResultAttachmentService(clinicalResultRepository, clinicalOrderItemRepository,
                clinicalServiceCatalogRepository, medicalAttachmentRepository, visitRepository, medicalRecordRepository,
                authorizationService, auditService, new ClinicalAttachmentValidator(properties()), storagePort, clock);
    }

    private ClinicalAttachmentProperties properties() {
        return new ClinicalAttachmentProperties(DataSize.ofMegabytes(10),
                List.of("image/jpeg", "image/png", "application/pdf"));
    }

    private UploadClinicalResultAttachmentCommand pdfCommand() {
        return new UploadClinicalResultAttachmentCommand("result.pdf", "application/pdf",
                "%PDF-1.7\nbody".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private Fixture fixture() {
        UUID actorId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        Visit visit = Visit.restore(visitId, "VIS-001", UUID.randomUUID(), UUID.randomUUID(), null, null,
                VisitType.WALK_IN, VisitStatus.IN_PROGRESS, NOW, NOW, null, "Consultation", null, actorId, NOW, null);
        MedicalRecord record = MedicalRecord.create(visitId, "Pain", null, null, null, null, null, null,
                "Stable", actorId, NOW);
        ClinicalResult result = ClinicalResult.create(itemId, visitId, ClinicalResultType.FILE, null, null, null,
                null, ClinicalResultAbnormalFlag.UNKNOWN, null, actorId, NOW);
        ClinicalOrderItem item = ClinicalOrderItem.restore(itemId, UUID.randomUUID(), serviceId, "LAB-PDF",
                "PDF result", null, ClinicalOrderItemStatus.PENDING, NOW, null);
        ClinicalServiceCatalog service = ClinicalServiceCatalog.restore(serviceId, "LAB-PDF", "PDF result",
                ClinicalServiceType.LAB_TEST, com.benhsoan.domain.clinical.enums.ClinicalResultDataType.FILE,
                null, null, null, true, NOW, null);
        return new Fixture(actorId, visit, record, result, item, service);
    }

    private record Fixture(UUID actorId, Visit visit, MedicalRecord record, ClinicalResult result,
            ClinicalOrderItem item, ClinicalServiceCatalog service) {}
}
