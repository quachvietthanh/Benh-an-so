package com.benhsoan.application.ucservice.clinical;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.clinical.ClinicalOrder;
import com.benhsoan.domain.clinical.ClinicalOrderItem;
import com.benhsoan.domain.clinical.enums.ClinicalOrderItemStatus;
import com.benhsoan.domain.clinical.enums.ClinicalOrderStatus;
import com.benhsoan.domain.clinical.exception.ClinicalOrderAlreadyCancelledException;
import com.benhsoan.domain.clinical.exception.ClinicalOrderAlreadyCompletedException;
import com.benhsoan.domain.clinical.exception.ClinicalOrderHasResultException;
import com.benhsoan.domain.clinical.exception.ClinicalOrderInvalidVisitException;
import com.benhsoan.domain.clinical.exception.ClinicalOrderItemNotFoundException;
import com.benhsoan.domain.clinical.exception.ClinicalOrderLockedMedicalRecordException;
import com.benhsoan.domain.clinical.exception.ClinicalOrderNotFoundException;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.medicalrecord.exception.MedicalRecordNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.visit.Visit;
import com.benhsoan.domain.visit.exception.VisitNotFoundException;
import com.benhsoan.port.dto.command.clinical.CancelClinicalOrderCommand;
import com.benhsoan.port.dto.command.clinical.CancelClinicalOrderItemCommand;
import com.benhsoan.port.dto.result.ClinicalOrderResult;
import com.benhsoan.port.inbound.clinical.CancelClinicalOrderUseCase;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderItemRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalOrderRepository;
import com.benhsoan.port.outbound.repository.clinical.ClinicalResultRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.visit.VisitRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CancelClinicalOrderService implements CancelClinicalOrderUseCase {

    private final ClinicalOrderRepository clinicalOrderRepository;
    private final ClinicalOrderItemRepository clinicalOrderItemRepository;
    private final ClinicalResultRepository clinicalResultRepository;
    private final VisitRepository visitRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final ClinicalOrderAuthorizationService authorizationService;
    private final ClinicalOrderAuditService auditService;
    private final ClinicalOrderResultMapper resultMapper;
    private final ClockPort clockPort;

    @Override
    public ClinicalOrderResult cancelOrder(CancelClinicalOrderCommand command) {
        if (command == null || command.orderId() == null) {
            throw new ValidationException("Clinical order id is required.");
        }
        String reason = validateReason(command.cancelReason());

        ClinicalOrder order = clinicalOrderRepository.findByIdForUpdate(command.orderId())
                .orElseThrow(() -> new ClinicalOrderNotFoundException(command.orderId()));

        if (order.getStatus() == ClinicalOrderStatus.COMPLETED) {
            throw new ClinicalOrderAlreadyCompletedException();
        }
        if (order.getStatus() == ClinicalOrderStatus.CANCELLED) {
            throw new ClinicalOrderAlreadyCancelledException();
        }

        Visit visit = visitRepository.findById(order.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(order.getVisitId()));
        if (!visit.isActive()) {
            throw new ClinicalOrderInvalidVisitException();
        }

        MedicalRecord medicalRecord = medicalRecordRepository.findByVisitId(visit.getId())
                .orElseThrow(() -> new MedicalRecordNotFoundException(visit.getId()));
        if (medicalRecord.isContentLocked()) {
            throw new ClinicalOrderLockedMedicalRecordException();
        }

        UUID actorId = authorizationService.requireCancelAccess(visit.getDoctorId(), order.getOrderedBy());

        List<ClinicalOrderItem> items = clinicalOrderItemRepository.findByClinicalOrderIdIn(List.of(order.getId()));

        for (ClinicalOrderItem item : items) {
            if (clinicalResultRepository.findByClinicalOrderItemId(item.getId()).isPresent()) {
                throw new ClinicalOrderHasResultException(
                        "Không thể hủy chỉ định '" + order.getOrderCode() + "' vì dịch vụ '" + item.getServiceName()
                                + "' đã có kết quả cận lâm sàng gắn với lượt khám (QTN-13)."
                );
            }
        }

        Instant now = clockPort.now();
        for (ClinicalOrderItem item : items) {
            if (item.getStatus() == ClinicalOrderItemStatus.PENDING) {
                item.cancel(reason, actorId, now);
            }
        }
        List<ClinicalOrderItem> savedItems = clinicalOrderItemRepository.saveAll(items);

        order.cancel(reason, actorId, now);
        ClinicalOrder savedOrder = clinicalOrderRepository.save(order);

        auditService.recordCancelled(visit.getPatientId(), visit.getId(), medicalRecord.getId(), actorId, reason, now);

        return resultMapper.toResult(savedOrder, savedItems);
    }

    @Override
    public ClinicalOrderResult cancelOrderItem(CancelClinicalOrderItemCommand command) {
        if (command == null || command.orderItemId() == null) {
            throw new ValidationException("Clinical order item id is required.");
        }
        String reason = validateReason(command.cancelReason());

        ClinicalOrderItem item = clinicalOrderItemRepository.findByIdForUpdate(command.orderItemId())
                .orElseThrow(() -> new ClinicalOrderItemNotFoundException(command.orderItemId()));

        ClinicalOrder order = clinicalOrderRepository.findByIdForUpdate(item.getClinicalOrderId())
                .orElseThrow(() -> new ClinicalOrderNotFoundException(item.getClinicalOrderId()));

        Visit visit = visitRepository.findById(order.getVisitId())
                .orElseThrow(() -> new VisitNotFoundException(order.getVisitId()));
        if (!visit.isActive()) {
            throw new ClinicalOrderInvalidVisitException();
        }

        MedicalRecord medicalRecord = medicalRecordRepository.findByVisitId(visit.getId())
                .orElseThrow(() -> new MedicalRecordNotFoundException(visit.getId()));
        if (medicalRecord.isContentLocked()) {
            throw new ClinicalOrderLockedMedicalRecordException();
        }

        UUID actorId = authorizationService.requireCancelAccess(visit.getDoctorId(), order.getOrderedBy());

        if (clinicalResultRepository.findByClinicalOrderItemId(item.getId()).isPresent()) {
            throw new ClinicalOrderHasResultException(
                    "Không thể hủy chỉ định dịch vụ '" + item.getServiceName()
                            + "' vì kết quả cận lâm sàng đã được nhập và gắn với lượt khám (QTN-13)."
            );
        }

        Instant now = clockPort.now();
        item.cancel(reason, actorId, now);
        clinicalOrderItemRepository.save(item);

        List<ClinicalOrderItem> allItems = clinicalOrderItemRepository.findByClinicalOrderIdIn(List.of(order.getId()));
        boolean allFinished = allItems.stream().allMatch(i ->
                i.getStatus() == ClinicalOrderItemStatus.COMPLETED || i.getStatus() == ClinicalOrderItemStatus.CANCELLED);
        boolean anyCompleted = allItems.stream().anyMatch(i -> i.getStatus() == ClinicalOrderItemStatus.COMPLETED);
        boolean allCancelled = allItems.stream().allMatch(i -> i.getStatus() == ClinicalOrderItemStatus.CANCELLED);

        if (allFinished) {
            if (anyCompleted) {
                order.complete(now);
            } else if (allCancelled) {
                order.cancel(reason, actorId, now);
            }
        }
        ClinicalOrder savedOrder = clinicalOrderRepository.save(order);

        auditService.recordCancelled(visit.getPatientId(), visit.getId(), medicalRecord.getId(), actorId, reason, now);

        return resultMapper.toResult(savedOrder, allItems);
    }

    private String validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("Cancellation reason is required.");
        }
        String trimmed = reason.trim();
        if (trimmed.length() > 500) {
            throw new ValidationException("Cancellation reason must not exceed 500 characters.");
        }
        return trimmed;
    }
}
