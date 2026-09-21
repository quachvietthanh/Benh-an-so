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
import com.benhsoan.port.dto.command.inventory.DiscardExpiredBatchCommand;
import com.benhsoan.port.dto.result.DiscardBatchResult;
import com.benhsoan.port.inbound.inventory.DiscardExpiredBatchUseCase;
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
public class DiscardExpiredBatchService implements DiscardExpiredBatchUseCase {

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
    public DiscardBatchResult discardExpired(DiscardExpiredBatchCommand command) {
        authorizer.requirePharmacistOrAdmin();
        validateCommand(command);

        UUID batchId = command.batchId();
        MedicineBatch batch = medicineBatchRepository.findByIdForUpdate(batchId)
                .orElseThrow(() -> new BatchNotFoundException(batchId));

        UUID medicineId = batch.getMedicineId();
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new MedicineNotFoundException(medicineId));

        Instant now = clockPort.now();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);

        Map<UUID, Integer> beforeEligibleQuantities = new HashMap<>(
                eligibleStockSnapshotService.snapshotEligibleStockQuantities(List.of(medicineId), today)
        );

        int discardedQuantity = batch.getQuantity();

        // Performs domain-level expiry check (expiryDate < today), status != EXPIRED, quantity > 0
        batch.discardExpired(today, now);
        medicineBatchRepository.save(batch);

        medicineRepository.updateStockQuantity(medicineId, -discardedQuantity);

        UUID performedBy = currentUserPort.getCurrentUserId();
        String trimmedReason = command.reason().trim();

        StockMovement movement = StockMovement.create(
                UUID.randomUUID(),
                medicineId,
                batch.getId(),
                StockMovementType.EXPIRE,
                StockMovementReferenceType.EXPIRY_PROCESS,
                batch.getId(),
                -discardedQuantity,
                discardedQuantity,
                0,
                performedBy,
                now,
                trimmedReason
        );
        stockMovementRepository.save(movement);

        lowStockAlertTransitionService.handleEligibleStockTransitions(
                List.of(medicineId),
                beforeEligibleQuantities,
                today,
                now
        );

        String detailJson = buildAuditDetail(batch, discardedQuantity, trimmedReason);
        auditLogRepository.save(AuditLog.create(
                performedBy,
                ActionType.UPDATE,
                ResourceType.MEDICINE,
                medicineId,
                detailJson,
                null,
                now
        ));

        return new DiscardBatchResult(
                batch.getId(),
                medicine.getId(),
                medicine.getMedicineCode(),
                medicine.getMedicineName(),
                batch.getBatchNumber(),
                batch.getExpiryDate(),
                discardedQuantity,
                batch.getStatus(),
                trimmedReason,
                performedBy,
                now
        );
    }

    private void validateCommand(DiscardExpiredBatchCommand command) {
        if (Objects.isNull(command)) {
            throw new ValidationException("Lệnh hủy lô thuốc không được null.");
        }
        if (Objects.isNull(command.batchId())) {
            throw new ValidationException("Mã lô thuốc không được để trống.");
        }
        if (Objects.isNull(command.reason()) || command.reason().isBlank()) {
            throw new ValidationException("Lý do hủy lô thuốc hết hạn không được để trống theo quy định QTN-32.");
        }
    }

    private String buildAuditDetail(
            MedicineBatch batch,
            int discardedQuantity,
            String reason
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("batchId", batch.getId().toString());
        detail.put("batchNumber", batch.getBatchNumber());
        detail.put("discardedQuantity", discardedQuantity);
        detail.put("reason", reason);
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize discard expired batch audit detail.", e);
        }
    }
}
