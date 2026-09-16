package com.benhsoan.application.ucservice.clinical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.clinical.ClinicalOrder;
import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.domain.clinical.enums.ClinicalOrderStatus;
import com.benhsoan.domain.clinical.exception.ClinicalOrderHasResultException;
import com.benhsoan.domain.clinical.exception.ClinicalOrderInvalidVisitException;
import com.benhsoan.domain.clinical.exception.ClinicalOrderItemInvalidStatusException;
import com.benhsoan.domain.clinical.exception.ClinicalOrderLockedMedicalRecordException;
import com.benhsoan.domain.clinical.exception.ClinicalOrderNotFoundException;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.enums.VisitStatus;
import com.benhsoan.domain.visit.enums.VisitType;
import com.benhsoan.port.dto.command.clinical.CancelClinicalOrderCommand;
import com.benhsoan.port.dto.command.clinical.CancelClinicalOrderItemCommand;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalResultRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelClinicalOrderService - Unit Tests (NCL-04-CN-008: TC-02, TC-04, QTN-13)")
class CancelClinicalOrderServiceTest {

        private static final Instant NOW = Instant.parse("2026-08-20T10:00:00Z");

        @Mock
        private ClinicalOrderRepository clinicalOrderRepository;
        @Mock
        private ClinicalOrderItemRepository clinicalOrderItemRepository;
        @Mock
        private ClinicalResultRepository clinicalResultRepository;
        @Mock
        private VisitRepository visitRepository;
        @Mock
        private MedicalRecordRepository medicalRecordRepository;
        @Mock
        private ClinicalOrderAuthorizationService authorizationService;
        @Mock
        private ClinicalOrderAuditService auditService;
        @Mock
        private ClockPort clockPort;
        @Spy
        private ClinicalOrderResultMapper resultMapper = new ClinicalOrderResultMapper();

        @InjectMocks
        private CancelClinicalOrderService service;

        private final UUID doctorId = UUID.randomUUID();
        private final UUID patientId = UUID.randomUUID();
        private final UUID visitId = UUID.randomUUID();
        private final UUID orderId = UUID.randomUUID();

        private Visit activeVisit() {
                return Visit.restore(
                                visitId, "VIS-001", patientId, doctorId, null, null,
                                VisitType.WALK_IN, VisitStatus.IN_PROGRESS, NOW, NOW, null,
                                "Consultation", null, doctorId, NOW, null);
        }

        private MedicalRecord editableMedicalRecord() {
                return MedicalRecord.create(visitId, "Headache", null, null, null, null, null, null, "Flu", doctorId,
                                NOW);
        }

        @Test
        @DisplayName("TC-02: Hủy toàn bộ chỉ định thành công khi chưa có kết quả -> Order & Items sang CANCELLED, lưu lý do và ghi audit")
        void tc02_cancelWholeOrderSuccessfully() {
                ClinicalOrder order = ClinicalOrder.restore(
                                orderId, "ORD-001", visitId, patientId, doctorId, doctorId, "Check up",
                                ClinicalOrderStatus.ORDERED, NOW, null, NOW, null);
                UUID item1Id = UUID.randomUUID();
                UUID item2Id = UUID.randomUUID();
                ClinicalOrderItem item1 = ClinicalOrderItem.restore(item1Id, orderId, UUID.randomUUID(), "XRAY",
                                "X-Ray", null, ClinicalOrderItemStatus.PENDING, NOW, null);
                ClinicalOrderItem item2 = ClinicalOrderItem.restore(item2Id, orderId, UUID.randomUUID(), "BLOOD",
                                "Blood test", null, ClinicalOrderItemStatus.PENDING, NOW, null);

                MedicalRecord record = editableMedicalRecord();
                when(authorizationService.requireCancelAccess(doctorId, doctorId)).thenReturn(doctorId);
                when(clinicalOrderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
                when(clinicalOrderItemRepository.findByClinicalOrderIdIn(List.of(orderId)))
                                .thenReturn(List.of(item1, item2));
                when(visitRepository.findById(visitId)).thenReturn(Optional.of(activeVisit()));
                when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.of(record));
                when(clinicalResultRepository.findByClinicalOrderItemId(item1Id)).thenReturn(Optional.empty());
                when(clinicalResultRepository.findByClinicalOrderItemId(item2Id)).thenReturn(Optional.empty());
                when(clockPort.now()).thenReturn(NOW);
                when(clinicalOrderRepository.save(any(ClinicalOrder.class))).thenAnswer(i -> i.getArgument(0));

                var result = service.cancelOrder(
                                new CancelClinicalOrderCommand(orderId, "Bệnh nhân không đồng ý thực hiện"));

                assertEquals("CANCELLED", result.status());
                assertEquals("Bệnh nhân không đồng ý thực hiện", result.cancelReason());
                assertNotNull(result.cancelledAt());
                result.items().forEach(item -> {
                        assertEquals("CANCELLED", item.status());
                        assertEquals("Bệnh nhân không đồng ý thực hiện", item.cancelReason());
                });

                verify(clinicalOrderItemRepository).saveAll(any());
                verify(auditService).recordCancelled(patientId, visitId, record.getId(), doctorId,
                                "Bệnh nhân không đồng ý thực hiện", NOW);
        }

        @Test
        @DisplayName("TC-04: Từ chối hủy chỉ định khi đã có kết quả cận lâm sàng (QTN-13) -> ném ClinicalOrderHasResultException")
        void tc04_cancelOrderRejectedWhenItemHasResult() {
                ClinicalOrder order = ClinicalOrder.restore(
                                orderId, "ORD-001", visitId, patientId, doctorId, doctorId, "Check up",
                                ClinicalOrderStatus.ORDERED, NOW, null, NOW, null);
                UUID item1Id = UUID.randomUUID();
                ClinicalOrderItem item1 = ClinicalOrderItem.restore(item1Id, orderId, UUID.randomUUID(), "BLOOD",
                                "Blood test", null, ClinicalOrderItemStatus.PENDING, NOW, null);

                when(authorizationService.requireCancelAccess(doctorId, doctorId)).thenReturn(doctorId);
                when(clinicalOrderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
                when(clinicalOrderItemRepository.findByClinicalOrderIdIn(List.of(orderId))).thenReturn(List.of(item1));
                when(visitRepository.findById(visitId)).thenReturn(Optional.of(activeVisit()));
                when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.of(editableMedicalRecord()));
                when(clinicalResultRepository.findByClinicalOrderItemId(item1Id))
                                .thenReturn(Optional.of(org.mockito.Mockito
                                                .mock(com.benhsoan.domain.clinical.ClinicalResult.class)));

                assertThrows(ClinicalOrderHasResultException.class,
                                () -> service.cancelOrder(new CancelClinicalOrderCommand(orderId, "Hủy xét nghiệm")));
        }

        @Test
        @DisplayName("Từ chối hủy khi bệnh án đã ký/khóa -> ném ClinicalOrderLockedMedicalRecordException")
        void cancelOrderRejectedWhenMedicalRecordIsLocked() {
                ClinicalOrder order = ClinicalOrder.restore(
                                orderId, "ORD-001", visitId, patientId, doctorId, doctorId, "Check up",
                                ClinicalOrderStatus.ORDERED, NOW, null, NOW, null);
                MedicalRecord record = editableMedicalRecord();
                record.sign("SIG", doctorId, NOW);

                when(clinicalOrderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
                when(visitRepository.findById(visitId)).thenReturn(Optional.of(activeVisit()));
                when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.of(record));

                assertThrows(ClinicalOrderLockedMedicalRecordException.class,
                                () -> service.cancelOrder(new CancelClinicalOrderCommand(orderId, "Hủy chỉ định")));
        }

        @Test
        @DisplayName("Từ chối hủy khi lượt khám không còn active -> ném ClinicalOrderInvalidVisitException")
        void cancelOrderRejectedWhenVisitIsNotActive() {
                ClinicalOrder order = ClinicalOrder.restore(
                                orderId, "ORD-001", visitId, patientId, doctorId, doctorId, "Check up",
                                ClinicalOrderStatus.ORDERED, NOW, null, NOW, null);
                Visit closedVisit = Visit.restore(
                                visitId, "VIS-001", patientId, doctorId, null, null,
                                VisitType.WALK_IN, VisitStatus.COMPLETED, NOW, NOW, NOW,
                                "Consultation", null, doctorId, NOW, NOW);

                when(clinicalOrderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
                when(visitRepository.findById(visitId)).thenReturn(Optional.of(closedVisit));

                assertThrows(ClinicalOrderInvalidVisitException.class,
                                () -> service.cancelOrder(new CancelClinicalOrderCommand(orderId, "Hủy chỉ định")));
        }

        @Test
        @DisplayName("Hủy từng dịch vụ (item-level): 1 item bị hủy, 1 item vẫn pending -> Order giữ trạng thái ORDERED")
        void cancelSingleItemLeavesOrderOrderedWhenOtherItemsPending() {
                ClinicalOrder order = ClinicalOrder.restore(
                                orderId, "ORD-001", visitId, patientId, doctorId, doctorId, "Check up",
                                ClinicalOrderStatus.ORDERED, NOW, null, NOW, null);
                UUID item1Id = UUID.randomUUID();
                UUID item2Id = UUID.randomUUID();
                ClinicalOrderItem item1 = ClinicalOrderItem.restore(item1Id, orderId, UUID.randomUUID(), "XRAY",
                                "X-Ray", null, ClinicalOrderItemStatus.PENDING, NOW, null);
                ClinicalOrderItem item2 = ClinicalOrderItem.restore(item2Id, orderId, UUID.randomUUID(), "BLOOD",
                                "Blood test", null, ClinicalOrderItemStatus.PENDING, NOW, null);

                MedicalRecord record = editableMedicalRecord();
                when(authorizationService.requireCancelAccess(doctorId, doctorId)).thenReturn(doctorId);
                when(clinicalOrderItemRepository.findByIdForUpdate(item1Id)).thenReturn(Optional.of(item1));
                when(clinicalOrderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
                when(clinicalOrderItemRepository.findByClinicalOrderIdIn(List.of(orderId)))
                                .thenReturn(List.of(item1, item2));
                when(visitRepository.findById(visitId)).thenReturn(Optional.of(activeVisit()));
                when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.of(record));
                when(clinicalResultRepository.findByClinicalOrderItemId(item1Id)).thenReturn(Optional.empty());
                when(clockPort.now()).thenReturn(NOW);
                when(clinicalOrderItemRepository.save(any(ClinicalOrderItem.class))).thenAnswer(i -> i.getArgument(0));
                when(clinicalOrderRepository.save(any(ClinicalOrder.class))).thenAnswer(i -> i.getArgument(0));

                var result = service
                                .cancelOrderItem(new CancelClinicalOrderItemCommand(item1Id, "Chỉ định nhầm dịch vụ"));

                assertEquals("ORDERED", result.status());
                verify(clinicalOrderItemRepository).save(item1);
                assertEquals(ClinicalOrderItemStatus.CANCELLED, item1.getStatus());
                assertEquals("Chỉ định nhầm dịch vụ", item1.getCancelReason());
                verify(auditService).recordCancelled(patientId, visitId, record.getId(), doctorId,
                                "Chỉ định nhầm dịch vụ", NOW);
        }

        @Test
        @DisplayName("Hủy item cuối cùng: tất cả items đều cancelled -> Order chuyển sang CANCELLED")
        void cancelLastItemTransitionsOrderToCancelled() {
                ClinicalOrder order = ClinicalOrder.restore(
                                orderId, "ORD-001", visitId, patientId, doctorId, doctorId, "Check up",
                                ClinicalOrderStatus.ORDERED, NOW, null, NOW, null);
                UUID item1Id = UUID.randomUUID();
                ClinicalOrderItem item1 = ClinicalOrderItem.restore(item1Id, orderId, UUID.randomUUID(), "XRAY",
                                "X-Ray", null, ClinicalOrderItemStatus.PENDING, NOW, null);

                MedicalRecord record = editableMedicalRecord();
                when(authorizationService.requireCancelAccess(doctorId, doctorId)).thenReturn(doctorId);
                when(clinicalOrderItemRepository.findByIdForUpdate(item1Id)).thenReturn(Optional.of(item1));
                when(clinicalOrderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
                when(clinicalOrderItemRepository.findByClinicalOrderIdIn(List.of(orderId))).thenReturn(List.of(item1));
                when(visitRepository.findById(visitId)).thenReturn(Optional.of(activeVisit()));
                when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.of(record));
                when(clinicalResultRepository.findByClinicalOrderItemId(item1Id)).thenReturn(Optional.empty());
                when(clockPort.now()).thenReturn(NOW);
                when(clinicalOrderItemRepository.save(any(ClinicalOrderItem.class))).thenAnswer(i -> i.getArgument(0));
                when(clinicalOrderRepository.save(any(ClinicalOrder.class))).thenAnswer(i -> i.getArgument(0));

                var result = service
                                .cancelOrderItem(new CancelClinicalOrderItemCommand(item1Id, "Bệnh nhân không làm"));

                assertEquals("CANCELLED", result.status());
        }

        @Test
        @DisplayName("Từ chối hủy item khi item đã ở trạng thái CANCELLED -> ném ClinicalOrderItemInvalidStatusException")
        void cancelAlreadyCancelledItemThrowsValidationException() {
                ClinicalOrder order = ClinicalOrder.restore(
                                orderId, "ORD-001", visitId, patientId, doctorId, doctorId, "Check up",
                                ClinicalOrderStatus.ORDERED, NOW, null, NOW, null);
                UUID item1Id = UUID.randomUUID();
                ClinicalOrderItem item1 = ClinicalOrderItem.restore(item1Id, orderId, UUID.randomUUID(), "XRAY",
                                "X-Ray", null,
                                ClinicalOrderItemStatus.CANCELLED, NOW, null, "Lý do cũ", doctorId, NOW);

                MedicalRecord record = editableMedicalRecord();
                when(authorizationService.requireCancelAccess(doctorId, doctorId)).thenReturn(doctorId);
                when(clinicalOrderItemRepository.findByIdForUpdate(item1Id)).thenReturn(Optional.of(item1));
                when(clinicalOrderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
                when(visitRepository.findById(visitId)).thenReturn(Optional.of(activeVisit()));
                when(medicalRecordRepository.findByVisitId(visitId)).thenReturn(Optional.of(record));
                when(clinicalResultRepository.findByClinicalOrderItemId(item1Id)).thenReturn(Optional.empty());

                assertThrows(ClinicalOrderItemInvalidStatusException.class,
                                () -> service.cancelOrderItem(new CancelClinicalOrderItemCommand(item1Id, "Hủy lại")));
        }

        @Test
        @DisplayName("Từ chối hủy khi không tìm thấy order -> ném ClinicalOrderNotFoundException")
        void orderNotFoundThrowsException() {
                UUID unknownOrderId = UUID.randomUUID();
                when(clinicalOrderRepository.findByIdForUpdate(unknownOrderId)).thenReturn(Optional.empty());

                assertThrows(ClinicalOrderNotFoundException.class,
                                () -> service.cancelOrder(new CancelClinicalOrderCommand(unknownOrderId, "Lý do")));
        }
}
