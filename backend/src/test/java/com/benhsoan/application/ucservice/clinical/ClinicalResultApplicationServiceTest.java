package com.benhsoan.application.ucservice.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.clinical.ClinicalOrder;
import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.domain.clinical.ClinicalResult;
import com.benhsoan.domain.clinical.ClinicalResultHistory;
import com.benhsoan.domain.clinical.ClinicalServiceCatalog;
import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.domain.clinical.enums.ClinicalOrderStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;
import com.benhsoan.domain.clinical.enums.ClinicalResultDataType;
import com.benhsoan.domain.clinical.enums.ClinicalResultStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;
import com.benhsoan.domain.clinical.exception.ClinicalOrderInvalidVisitException;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.command.clinical.EnterClinicalResultCommand;
import com.benhsoan.port.dto.command.clinical.UpdateClinicalResultCommand;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalResultHistoryRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalResultRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalServiceCatalogRepository;
import com.benhsoan.port.outbound.repository.clinical.MedicalAttachmentRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class ClinicalResultApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-20T01:00:00Z");

    @Mock private ClinicalOrderItemRepository clinicalOrderItemRepository;
    @Mock private ClinicalOrderRepository clinicalOrderRepository;
    @Mock private ClinicalServiceCatalogRepository clinicalServiceCatalogRepository;
    @Mock private ClinicalResultRepository clinicalResultRepository;
    @Mock private ClinicalResultHistoryRepository clinicalResultHistoryRepository;
    @Mock private MedicalAttachmentRepository medicalAttachmentRepository;
    @Mock private VisitRepository visitRepository;
    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private ClinicalOrderAuthorizationService authorizationService;
    @Mock private ClinicalResultAuditService auditService;
    @Mock private ClockPort clock;

    @InjectMocks private ClinicalResultService clinicalResultService;

    @Test
    void rejectsFinalizingFileResultWithoutUploadedAttachment() {
        Fixture fixture = fixture();
        ClinicalResult result = ClinicalResult.create(fixture.item().getId(), fixture.visit().getId(),
                ClinicalResultType.FILE, null, null, null, null, null, null, fixture.actorId(), NOW);
        when(authorizationService.requireWriteAccess()).thenReturn(fixture.actorId());
        when(clinicalResultRepository.findById(result.getId())).thenReturn(Optional.of(result));
        when(visitRepository.findById(fixture.visit().getId())).thenReturn(Optional.of(fixture.visit()));
        when(medicalRecordRepository.findByVisitId(fixture.visit().getId())).thenReturn(Optional.of(fixture.record()));
        when(medicalAttachmentRepository.existsByClinicalResultId(result.getId())).thenReturn(false);

        assertThrows(ValidationException.class, () -> clinicalResultService.finalizeResult(result.getId()));
    }

    @Test
    void rejectsResultForInactiveVisit() {
        Fixture fixture = fixture();
        Visit cancelled = Visit.restore(fixture.visit().getId(), "VIS-001", fixture.visit().getPatientId(),
                UUID.randomUUID(), null, null, VisitType.WALK_IN, VisitStatus.CANCELLED, NOW, null, NOW,
                "Consultation", null, fixture.actorId(), NOW, null);
        when(authorizationService.requireWriteAccess()).thenReturn(fixture.actorId());
        when(clinicalOrderItemRepository.findById(fixture.item().getId())).thenReturn(Optional.of(fixture.item()));
        when(clinicalResultRepository.findByClinicalOrderItemId(fixture.item().getId())).thenReturn(Optional.empty());
        when(clinicalOrderRepository.findById(fixture.order().getId())).thenReturn(Optional.of(fixture.order()));
        when(visitRepository.findById(fixture.visit().getId())).thenReturn(Optional.of(cancelled));

        assertThrows(ClinicalOrderInvalidVisitException.class, () -> clinicalResultService.enter(fixture.item().getId(),
                new EnterClinicalResultCommand(BigDecimal.TEN, null, ClinicalResultAbnormalFlag.NORMAL, null)));
    }

    @Test
    void finalizesResultCreatesFullHistoryAndCompletesOrder() {
        Fixture fixture = fixture();
        ClinicalResult result = ClinicalResult.create(fixture.item().getId(), fixture.visit().getId(),
                ClinicalResultType.NUMBER, BigDecimal.TEN, null, "mmol/L", "3.9-6.4",
                ClinicalResultAbnormalFlag.HIGH, "Follow up", fixture.actorId(), NOW);
        when(authorizationService.requireWriteAccess()).thenReturn(fixture.actorId());
        when(clinicalResultRepository.findById(result.getId())).thenReturn(Optional.of(result));
        when(visitRepository.findById(fixture.visit().getId())).thenReturn(Optional.of(fixture.visit()));
        when(medicalRecordRepository.findByVisitId(fixture.visit().getId())).thenReturn(Optional.of(fixture.record()));
        when(clock.now()).thenReturn(NOW.plusSeconds(60));
        when(clinicalResultRepository.save(any(ClinicalResult.class))).thenAnswer(call -> call.getArgument(0));
        when(clinicalOrderItemRepository.findById(fixture.item().getId())).thenReturn(Optional.of(fixture.item()));
        when(clinicalOrderItemRepository.save(any(ClinicalOrderItem.class))).thenAnswer(call -> call.getArgument(0));
        when(clinicalOrderRepository.findById(fixture.order().getId())).thenReturn(Optional.of(fixture.order()));
        when(clinicalOrderItemRepository.findByClinicalOrderIdIn(List.of(fixture.order().getId())))
                .thenReturn(List.of(fixture.item()));
        when(clinicalOrderRepository.save(any(ClinicalOrder.class))).thenAnswer(call -> call.getArgument(0));

        var response = clinicalResultService.finalizeResult(result.getId());

        ArgumentCaptor<ClinicalResultHistory> historyCaptor = ArgumentCaptor.forClass(ClinicalResultHistory.class);
        verify(clinicalResultHistoryRepository).save(historyCaptor.capture());
        assertEquals(ClinicalResultStatus.FINAL, response.status());
        assertEquals(ClinicalResultStatus.DRAFT, historyCaptor.getValue().getOldStatus());
        assertEquals(ClinicalResultStatus.FINAL, historyCaptor.getValue().getNewStatus());
        assertEquals(BigDecimal.TEN, historyCaptor.getValue().getOldNumericValue());
        assertEquals(ClinicalOrderItemStatus.COMPLETED, fixture.item().getStatus());
        assertEquals(ClinicalOrderStatus.COMPLETED, fixture.order().getStatus());
    }

    @Test
    void delegatesFinalResultUpdateRuleToDomain() {
        Fixture fixture = fixture();
        ClinicalResult result = ClinicalResult.create(fixture.item().getId(), fixture.visit().getId(),
                ClinicalResultType.NUMBER, BigDecimal.ONE, null, null, null, null, null, fixture.actorId(), NOW);
        result.finalizeResult(fixture.actorId(), NOW);
        when(authorizationService.requireWriteAccess()).thenReturn(fixture.actorId());
        when(clinicalResultRepository.findById(result.getId())).thenReturn(Optional.of(result));
        when(visitRepository.findById(fixture.visit().getId())).thenReturn(Optional.of(fixture.visit()));
        when(medicalRecordRepository.findByVisitId(fixture.visit().getId())).thenReturn(Optional.of(fixture.record()));
        when(clock.now()).thenReturn(NOW);

        assertThrows(com.benhsoan.domain.clinical.exception.ClinicalResultAlreadyFinalizedException.class,
                () -> clinicalResultService.update(result.getId(), new UpdateClinicalResultCommand(BigDecimal.TEN,
                        null, null, null, "Correct input")));
    }

    private Fixture arrangeEnter(ClinicalResultDataType dataType) {
        Fixture fixture = fixture();
        when(authorizationService.requireWriteAccess()).thenReturn(fixture.actorId());
        when(clinicalOrderItemRepository.findById(fixture.item().getId())).thenReturn(Optional.of(fixture.item()));
        when(clinicalResultRepository.findByClinicalOrderItemId(fixture.item().getId())).thenReturn(Optional.empty());
        when(clinicalOrderRepository.findById(fixture.order().getId())).thenReturn(Optional.of(fixture.order()));
        when(visitRepository.findById(fixture.visit().getId())).thenReturn(Optional.of(fixture.visit()));
        when(medicalRecordRepository.findByVisitId(fixture.visit().getId())).thenReturn(Optional.of(fixture.record()));
        when(clinicalServiceCatalogRepository.findById(fixture.item().getClinicalServiceId()))
                .thenReturn(Optional.of(ClinicalServiceCatalog.restore(fixture.item().getClinicalServiceId(), "LAB-GLU",
                        "Blood glucose", ClinicalServiceType.LAB_TEST, dataType, "mmol/L", "3.9-6.4", null,
                        true, NOW, null)));
        return fixture;
    }

    private Fixture fixture() {
        UUID actorId = UUID.randomUUID();
        UUID visitId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Visit visit = Visit.restore(visitId, "VIS-001", UUID.randomUUID(), UUID.randomUUID(), null, null,
                VisitType.WALK_IN, VisitStatus.IN_PROGRESS, NOW, NOW, null, "Consultation", null, actorId, NOW, null);
        MedicalRecord record = MedicalRecord.create(visitId, "Headache", null, null, null, null, null, null,
                "Stable", actorId, NOW);
        ClinicalOrder order = ClinicalOrder.restore(orderId, "ORD-001", visitId, record.getId(), visit.getPatientId(),
                actorId, null, ClinicalOrderStatus.ORDERED, NOW, null, NOW, null);
        ClinicalOrderItem item = ClinicalOrderItem.restore(UUID.randomUUID(), orderId, UUID.randomUUID(), "LAB-GLU",
                "Blood glucose", null, ClinicalOrderItemStatus.PENDING, NOW, null);
        return new Fixture(actorId, visit, record, order, item);
    }

    private record Fixture(UUID actorId, Visit visit, MedicalRecord record, ClinicalOrder order, ClinicalOrderItem item) {}
}
