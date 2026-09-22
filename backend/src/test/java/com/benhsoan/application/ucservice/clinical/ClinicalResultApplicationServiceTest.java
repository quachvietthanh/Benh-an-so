package com.benhsoan.application.ucservice.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.application.ucservice.portal.PatientPortalNotificationCreator;
import com.benhsoan.domain.clinical.ClinicalOrder;
import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.domain.clinical.ClinicalResult;
import com.benhsoan.domain.clinical.ClinicalResultHistory;
import com.benhsoan.domain.clinical.ClinicalReferenceRange;
import com.benhsoan.domain.clinical.ClinicalServiceCatalog;
import com.benhsoan.domain.clinical.ReferenceRangeEvaluator;
import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.domain.clinical.enums.ClinicalOrderStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;
import com.benhsoan.domain.clinical.enums.ClinicalResultStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultDataType;
import com.benhsoan.domain.clinical.enums.ClinicalResultType;
import com.benhsoan.domain.clinical.enums.ClinicalServiceType;
import com.benhsoan.domain.clinical.exception.ClinicalOrderInvalidVisitException;
import com.benhsoan.domain.clinical.exception.ClinicalOrderItemNotFoundException;
import com.benhsoan.domain.clinical.exception.ClinicalResultNotFoundException;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.patient.Patient;
import com.benhsoan.domain.patient.enums.Gender;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.command.clinical.EnterClinicalResultCommand;
import com.benhsoan.port.dto.command.clinical.UpdateClinicalResultCommand;
import com.benhsoan.port.dto.result.ClinicalResultResult;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalReferenceRangeRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalResultHistoryRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalResultRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalServiceCatalogRepository;
import com.benhsoan.port.outbound.repository.clinical.MedicalAttachmentRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.patient.PatientRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class ClinicalResultApplicationServiceTest {

        private static final Instant NOW = Instant.parse("2026-08-20T01:00:00Z");

        @Mock
        private ClinicalOrderItemRepository clinicalOrderItemRepository;
        @Mock
        private ClinicalOrderRepository clinicalOrderRepository;
        @Mock
        private ClinicalServiceCatalogRepository clinicalServiceCatalogRepository;
        @Mock
        private ClinicalResultRepository clinicalResultRepository;
        @Mock
        private ClinicalResultHistoryRepository clinicalResultHistoryRepository;
        @Mock
        private MedicalAttachmentRepository medicalAttachmentRepository;
        @Mock
        private VisitRepository visitRepository;
        @Mock
        private MedicalRecordRepository medicalRecordRepository;
        @Mock
        private PatientRepository patientRepository;
        @Mock
        private ClinicalReferenceRangeRepository clinicalReferenceRangeRepository;
        @Spy
        private ReferenceRangeEvaluator referenceRangeEvaluator = new ReferenceRangeEvaluator();
        @Mock
        private ClinicalOrderAuthorizationService authorizationService;
        @Mock
        private ClinicalResultAuditService auditService;
        @Mock
        private PatientPortalNotificationCreator patientPortalNotificationCreator;
        @Mock
        private ClockPort clock;

        @InjectMocks
        private ClinicalResultService clinicalResultService;

        @Test
        void rejectsFinalizingFileResultWithoutUploadedAttachment() {
                Fixture fixture = fixture();
                ClinicalResult result = ClinicalResult.create(fixture.item().getId(), fixture.visit().getId(),
                                ClinicalResultType.FILE, null, null, null, null, null, null, fixture.actorId(), NOW);
                when(authorizationService.requireWriteAccess()).thenReturn(fixture.actorId());
                when(clinicalResultRepository.findById(result.getId())).thenReturn(Optional.of(result));
                when(visitRepository.findById(fixture.visit().getId())).thenReturn(Optional.of(fixture.visit()));
                when(medicalRecordRepository.findByVisitId(fixture.visit().getId()))
                                .thenReturn(Optional.of(fixture.record()));
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
                when(clinicalOrderItemRepository.findByIdForUpdate(fixture.item().getId()))
                                .thenReturn(Optional.of(fixture.item()));
                when(clinicalResultRepository.findByClinicalOrderItemId(fixture.item().getId()))
                                .thenReturn(Optional.empty());
                when(clinicalOrderRepository.findById(fixture.order().getId()))
                                .thenReturn(Optional.of(fixture.order()));
                when(visitRepository.findById(fixture.visit().getId())).thenReturn(Optional.of(cancelled));

                assertThrows(ClinicalOrderInvalidVisitException.class,
                                () -> clinicalResultService.enter(fixture.item().getId(),
                                                new EnterClinicalResultCommand(BigDecimal.TEN, null,
                                                                ClinicalResultAbnormalFlag.NORMAL, null)));
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
                when(medicalRecordRepository.findByVisitId(fixture.visit().getId()))
                                .thenReturn(Optional.of(fixture.record()));
                when(clock.now()).thenReturn(NOW.plusSeconds(60));
                when(clinicalResultRepository.save(any(ClinicalResult.class))).thenAnswer(call -> call.getArgument(0));
                when(clinicalOrderItemRepository.findById(fixture.item().getId()))
                                .thenReturn(Optional.of(fixture.item()));
                when(clinicalOrderItemRepository.save(any(ClinicalOrderItem.class)))
                                .thenAnswer(call -> call.getArgument(0));
                when(clinicalOrderRepository.findById(fixture.order().getId()))
                                .thenReturn(Optional.of(fixture.order()));
                when(clinicalOrderItemRepository.findByClinicalOrderIdIn(List.of(fixture.order().getId())))
                                .thenReturn(List.of(fixture.item()));
                when(clinicalOrderRepository.save(any(ClinicalOrder.class))).thenAnswer(call -> call.getArgument(0));

                var response = clinicalResultService.finalizeResult(result.getId());

                ArgumentCaptor<ClinicalResultHistory> historyCaptor = ArgumentCaptor
                                .forClass(ClinicalResultHistory.class);
                verify(clinicalResultHistoryRepository).save(historyCaptor.capture());
                verify(patientPortalNotificationCreator).createLabResultAvailable(
                                fixture.visit().getPatientId(), result.getId(), NOW.plusSeconds(60));
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
                                ClinicalResultType.NUMBER, BigDecimal.ONE, null, null, null, null, null,
                                fixture.actorId(), NOW);
                result.finalizeResult(fixture.actorId(), NOW);
                when(authorizationService.requireWriteAccess()).thenReturn(fixture.actorId());
                when(clinicalResultRepository.findById(result.getId())).thenReturn(Optional.of(result));
                when(visitRepository.findById(fixture.visit().getId())).thenReturn(Optional.of(fixture.visit()));
                when(medicalRecordRepository.findByVisitId(fixture.visit().getId()))
                                .thenReturn(Optional.of(fixture.record()));
                when(clock.now()).thenReturn(NOW);

                assertThrows(com.benhsoan.domain.clinical.exception.ClinicalResultAlreadyFinalizedException.class,
                                () -> clinicalResultService.update(result.getId(),
                                                new UpdateClinicalResultCommand(BigDecimal.TEN,
                                                                null, null, null, "Correct input")));
        }

        @Test
        void returnsNotFoundExceptionsForRequestedClinicalResultResources() {
                UUID clinicalResultId = UUID.randomUUID();
                UUID clinicalOrderItemId = UUID.randomUUID();
                when(authorizationService.requireReadAccess()).thenReturn(UUID.randomUUID());
                when(clinicalResultRepository.findById(clinicalResultId)).thenReturn(Optional.empty());
                when(authorizationService.requireWriteAccess()).thenReturn(UUID.randomUUID());
                when(clinicalOrderItemRepository.findByIdForUpdate(clinicalOrderItemId)).thenReturn(Optional.empty());

                assertThrows(ClinicalResultNotFoundException.class,
                                () -> clinicalResultService.getById(clinicalResultId));
                assertThrows(ClinicalOrderItemNotFoundException.class,
                                () -> clinicalResultService.enter(clinicalOrderItemId,
                                                new EnterClinicalResultCommand(BigDecimal.TEN, null,
                                                                ClinicalResultAbnormalFlag.NORMAL, null)));
        }

        @Test
        void treatsMissingRelationsOfAnExistingResultAsInfrastructureFailure() {
                Fixture fixture = fixture();
                ClinicalResult result = ClinicalResult.create(fixture.item().getId(), fixture.visit().getId(),
                                ClinicalResultType.NUMBER, BigDecimal.ONE, null, null, null, null, null,
                                fixture.actorId(), NOW);
                when(authorizationService.requireWriteAccess()).thenReturn(fixture.actorId());
                when(clinicalResultRepository.findById(result.getId())).thenReturn(Optional.of(result));
                when(visitRepository.findById(fixture.visit().getId())).thenReturn(Optional.empty());

                assertThrows(IllegalStateException.class, () -> clinicalResultService.update(result.getId(),
                                new UpdateClinicalResultCommand(BigDecimal.TEN, null, null, null, "Correct input")));
        }

        @Test
        void keepsOriginalThresholdSnapshotButNewResultsUseUpdatedThreshold() {
                UUID serviceId = UUID.randomUUID();
                UUID patientId = UUID.randomUUID();
                Patient patient = patient(patientId);
                ClinicalServiceCatalog service = numberService(serviceId);
                ClinicalReferenceRange originalRange = ClinicalReferenceRange.restore(UUID.randomUUID(), serviceId,
                                Gender.MALE, 18, 64, new BigDecimal("10"), new BigDecimal("20"), true, NOW, null);
                ClinicalReferenceRange updatedRange = ClinicalReferenceRange.restore(UUID.randomUUID(), serviceId,
                                Gender.MALE, 18, 64, new BigDecimal("10"), new BigDecimal("30"), true, NOW, null);

                // 1) Enter the old result while the active threshold is 10..20.
                Fixture oldFixture = fixture(serviceId, patientId);
                stubEnter(oldFixture, serviceId, service, patient, List.of(originalRange), NOW);

                ClinicalResultResult oldResult = clinicalResultService.enter(oldFixture.item().getId(),
                                new EnterClinicalResultCommand(new BigDecimal("25"), null,
                                                ClinicalResultAbnormalFlag.NORMAL, null));

                assertEquals(new BigDecimal("10"), oldResult.lowerBound());
                assertEquals(new BigDecimal("20"), oldResult.upperBound());
                assertEquals(ClinicalResultAbnormalFlag.HIGH, oldResult.abnormalFlag());

                ArgumentCaptor<ClinicalResult> captor = ArgumentCaptor.forClass(ClinicalResult.class);
                verify(clinicalResultRepository).save(captor.capture());
                ClinicalResult persistedOld = captor.getValue();

                // 2) The threshold changes to 10..30. Updating the OLD result must keep its snapshot.
                when(clinicalResultRepository.findById(persistedOld.getId())).thenReturn(Optional.of(persistedOld));

                ClinicalResultResult updatedOld = clinicalResultService.update(persistedOld.getId(),
                                new UpdateClinicalResultCommand(new BigDecimal("25"), null,
                                                ClinicalResultAbnormalFlag.NORMAL, null, "Recheck"));

                assertEquals(new BigDecimal("10"), updatedOld.lowerBound());
                assertEquals(new BigDecimal("20"), updatedOld.upperBound());
                assertEquals(ClinicalResultAbnormalFlag.HIGH, updatedOld.abnormalFlag());

                // The update path must NOT re-resolve the current catalog threshold.
                verify(clinicalReferenceRangeRepository, times(1)).findActiveByClinicalServiceId(any());

                // 3) A NEW result now resolves the updated threshold 10..30.
                Fixture newFixture = fixture(serviceId, patientId);
                stubEnter(newFixture, serviceId, service, patient, List.of(updatedRange), NOW);

                ClinicalResultResult newResult = clinicalResultService.enter(newFixture.item().getId(),
                                new EnterClinicalResultCommand(new BigDecimal("25"), null,
                                                ClinicalResultAbnormalFlag.NORMAL, null));

                assertEquals(new BigDecimal("10"), newResult.lowerBound());
                assertEquals(new BigDecimal("30"), newResult.upperBound());
                assertEquals(ClinicalResultAbnormalFlag.NORMAL, newResult.abnormalFlag());

                verify(clinicalReferenceRangeRepository, times(2)).findActiveByClinicalServiceId(any());
        }

        @Test
        void resolvesAgeUsingClinicTimezoneAtUtcDateBoundary() {
                UUID serviceId = UUID.randomUUID();
                UUID patientId = UUID.randomUUID();
                Patient patient = patient(patientId, LocalDate.of(2008, 9, 14));
                ClinicalServiceCatalog service = numberService(serviceId);
                ClinicalReferenceRange age17 = ClinicalReferenceRange.restore(UUID.randomUUID(), serviceId,
                                Gender.MALE, 17, 17, new BigDecimal("1"), new BigDecimal("2"), true, NOW, null);
                ClinicalReferenceRange age18 = ClinicalReferenceRange.restore(UUID.randomUUID(), serviceId,
                                Gender.MALE, 18, 18, new BigDecimal("10"), new BigDecimal("20"), true, NOW, null);
                Fixture fixture = fixture(serviceId, patientId);
                // 2026-09-13T18:30:00Z == 2026-09-14 01:30 in Asia/Ho_Chi_Minh (UTC+7),
                // but still 2026-09-13 under UTC. Only the clinic timezone yields age 18.
                Instant entryNow = Instant.parse("2026-09-13T18:30:00Z");

                TimeZone original = TimeZone.getDefault();
                try {
                        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
                        stubEnter(fixture, serviceId, service, patient, List.of(age17, age18), entryNow);

                        ClinicalResultResult result = clinicalResultService.enter(fixture.item().getId(),
                                        new EnterClinicalResultCommand(new BigDecimal("25"), null,
                                                        ClinicalResultAbnormalFlag.NORMAL, null));

                        assertEquals(new BigDecimal("10"), result.lowerBound());
                        assertEquals(new BigDecimal("20"), result.upperBound());
                        assertEquals(ClinicalResultAbnormalFlag.HIGH, result.abnormalFlag());
                } finally {
                        TimeZone.setDefault(original);
                }
        }

        private void stubEnter(Fixture fixture, UUID serviceId, ClinicalServiceCatalog service, Patient patient,
                        List<ClinicalReferenceRange> activeRanges, Instant entryNow) {
                when(authorizationService.requireWriteAccess()).thenReturn(fixture.actorId());
                when(clinicalOrderItemRepository.findByIdForUpdate(fixture.item().getId()))
                                .thenReturn(Optional.of(fixture.item()));
                when(clinicalResultRepository.findByClinicalOrderItemId(fixture.item().getId()))
                                .thenReturn(Optional.empty());
                when(clinicalOrderRepository.findById(fixture.order().getId()))
                                .thenReturn(Optional.of(fixture.order()));
                when(visitRepository.findById(fixture.visit().getId()))
                                .thenReturn(Optional.of(fixture.visit()));
                when(medicalRecordRepository.findByVisitId(fixture.visit().getId()))
                                .thenReturn(Optional.of(fixture.record()));
                when(clinicalServiceCatalogRepository.findById(serviceId)).thenReturn(Optional.of(service));
                when(patientRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
                when(clinicalReferenceRangeRepository.findActiveByClinicalServiceId(serviceId))
                                .thenReturn(activeRanges);
                when(clock.now()).thenReturn(entryNow);
                when(clinicalResultRepository.save(any(ClinicalResult.class)))
                                .thenAnswer(call -> call.getArgument(0));
        }

        private Fixture fixture(UUID serviceId, UUID patientId) {
                UUID actorId = UUID.randomUUID();
                UUID visitId = UUID.randomUUID();
                UUID orderId = UUID.randomUUID();
                Visit visit = Visit.restore(visitId, "VIS-001", patientId, UUID.randomUUID(), null, null,
                                VisitType.WALK_IN, VisitStatus.IN_PROGRESS, NOW, NOW, null, "Consultation", null,
                                actorId, NOW, null);
                MedicalRecord record = MedicalRecord.create(visitId, "Headache", null, null, null, null, null, null,
                                "Stable", actorId, NOW);
                ClinicalOrder order = ClinicalOrder.restore(orderId, "ORD-001", visitId, record.getId(),
                                visit.getPatientId(), actorId, null, ClinicalOrderStatus.ORDERED, NOW, null, NOW, null);
                ClinicalOrderItem item = ClinicalOrderItem.restore(UUID.randomUUID(), orderId, serviceId,
                                "LAB-GLU", "Blood glucose", null, ClinicalOrderItemStatus.PENDING, NOW, null);
                return new Fixture(actorId, visit, record, order, item);
        }

        private ClinicalServiceCatalog numberService(UUID serviceId) {
                return ClinicalServiceCatalog.restore(serviceId, UUID.randomUUID(), "LAB-GLU", "Blood glucose",
                                ClinicalServiceType.LAB_TEST, ClinicalResultDataType.NUMBER, "mmol/L", "3.9-5.5",
                                "desc", true, NOW, null);
        }

        private Patient patient(UUID patientId) {
                return patient(patientId, LocalDate.of(2000, 1, 1));
        }

        private Patient patient(UUID patientId, LocalDate dateOfBirth) {
                return Patient.restore(patientId, "PAT-001", "Test Patient", dateOfBirth, Gender.MALE,
                                null, null, null, null, null, null, null, null, true, NOW, null, null,
                                UUID.randomUUID());
        }

        private Fixture fixture() {
                UUID actorId = UUID.randomUUID();
                UUID visitId = UUID.randomUUID();
                UUID orderId = UUID.randomUUID();
                Visit visit = Visit.restore(visitId, "VIS-001", UUID.randomUUID(), UUID.randomUUID(), null, null,
                                VisitType.WALK_IN, VisitStatus.IN_PROGRESS, NOW, NOW, null, "Consultation", null,
                                actorId, NOW, null);
                MedicalRecord record = MedicalRecord.create(visitId, "Headache", null, null, null, null, null, null,
                                "Stable", actorId, NOW);
                ClinicalOrder order = ClinicalOrder.restore(orderId, "ORD-001", visitId, record.getId(),
                                visit.getPatientId(),
                                actorId, null, ClinicalOrderStatus.ORDERED, NOW, null, NOW, null);
                ClinicalOrderItem item = ClinicalOrderItem.restore(UUID.randomUUID(), orderId, UUID.randomUUID(),
                                "LAB-GLU",
                                "Blood glucose", null, ClinicalOrderItemStatus.PENDING, NOW, null);
                return new Fixture(actorId, visit, record, order, item);
        }

        private record Fixture(UUID actorId, Visit visit, MedicalRecord record, ClinicalOrder order,
                        ClinicalOrderItem item) {
        }
}
