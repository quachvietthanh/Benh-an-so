package com.benhsoan.application.ucservice.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.inventory.StockMovement;
import com.benhsoan.domain.inventory.enums.StockMovementReferenceType;
import com.benhsoan.domain.inventory.enums.StockMovementType;
import com.benhsoan.domain.inventory.exception.BatchNotFoundException;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicine.exception.MedicineNotFoundException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.inventory.AdjustBatchStockCommand;
import com.benhsoan.port.dto.result.BatchAdjustmentResult;
import com.benhsoan.port.inbound.inventory.AdjustBatchStockUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.inventory.StockMovementRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AdjustBatchStockService implements AdjustBatchStockUseCase {

    private final MedicineBatchRepository medicineBatchRepository;
    private final MedicineRepository medicineRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryManagementAuthorizer authorizer;
    private final EligibleStockSnapshotService eligibleStockSnapshotService;
    private final LowStockAlertTransitionService lowStockAlertTransitionService;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final ObjectMapper objectMapper;

    @Override
    public BatchAdjustmentResult adjustStock(AdjustBatchStockCommand command) {
        authorizer.requireInventoryUpdate();
        validateCommand(command);

        UUID batchId = command.batchId();
        MedicineBatch batch = medicineBatchRepository.findByIdForUpdate(batchId)
                .orElseThrow(() -> new BatchNotFoundException(batchId));

        int quantityBefore = batch.getQuantity();
        int quantityAfter = command.actualQuantity();
        int quantityChange = quantityAfter - quantityBefore;

        if (quantityChange == 0) {
            throw new ValidationException(
                    "Số lượng kiểm kê thực tế trùng khớp với tồn kho hiện tại, không có chênh lệch để điều chỉnh.");
        }

        Instant now = clockPort.now();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);

        UUID medicineId = batch.getMedicineId();
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new MedicineNotFoundException(medicineId));

        Map<UUID, Integer> beforeEligibleQuantities = new HashMap<>(
                eligibleStockSnapshotService.snapshotEligibleStockQuantities(List.of(medicineId), today));

        batch.adjustStock(quantityAfter, today, now);

        medicineBatchRepository.save(batch);

        medicineRepository.updateStockQuantity(medicineId, quantityChange);

        UUID performedBy = currentUserPort.getCurrentUserId();
        String trimmedReason = command.reason().trim();

        StockMovement movement = StockMovement.create(
                UUID.randomUUID(),
                medicineId,
                batch.getId(),
                StockMovementType.ADJUSTMENT,
                StockMovementReferenceType.MANUAL_ADJUSTMENT,
                batch.getId(),
                quantityChange,
                quantityBefore,
                quantityAfter,
                performedBy,
                now,
                trimmedReason);
        stockMovementRepository.save(movement);

        lowStockAlertTransitionService.handleEligibleStockTransitions(
                List.of(medicineId),
                beforeEligibleQuantities,
                today,
                now);

        String detailJson = buildAuditDetail(batch, quantityBefore, quantityAfter, quantityChange, trimmedReason);
        auditLogRepository.save(AuditLog.create(
                performedBy,
                ActionType.UPDATE,
                ResourceType.MEDICINE,
                medicineId,
                detailJson,
                null,
                now));

        return new BatchAdjustmentResult(
                batch.getId(),
                medicine.getId(),
                medicine.getMedicineCode(),
                medicine.getMedicineName(),
                batch.getBatchNumber(),
                batch.getExpiryDate(),
                quantityBefore,
                quantityAfter,
                quantityChange,
                batch.getStatus(),
                trimmedReason,
                performedBy,
                now);
    }

    private void validateCommand(AdjustBatchStockCommand command) {
        if (Objects.isNull(command)) {
            throw new ValidationException("Lệnh điều chỉnh tồn kho không được null.");
        }
        if (Objects.isNull(command.batchId())) {
            throw new ValidationException("Mã lô thuốc không được để trống.");
        }
        if (Objects.isNull(command.reason()) || command.reason().isBlank()) {
            throw new ValidationException("Lý do điều chỉnh tồn kho không được để trống theo quy định QTN-32.");
        }
        if (command.actualQuantity() < 0) {
            throw new ValidationException("Số lượng tồn kho thực tế không được âm.");
        }
    }

    private String buildAuditDetail(
            MedicineBatch batch,
            int quantityBefore,
            int quantityAfter,
            int quantityChange,
            String reason) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("batchId", batch.getId().toString());
        detail.put("batchNumber", batch.getBatchNumber());
        detail.put("quantityBefore", quantityBefore);
        detail.put("quantityAfter", quantityAfter);
        detail.put("quantityChange", quantityChange);
        detail.put("reason", reason);
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize inventory adjustment audit detail.", e);
        }
    }
}
