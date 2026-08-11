package com.benhsoan.application.ucservice.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.inventory.InventoryReceipt;
import com.benhsoan.domain.inventory.InventoryReceiptItem;
import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.inventory.ReceiveStockCommand;
import com.benhsoan.port.dto.command.inventory.ReceiveStockItemCommand;
import com.benhsoan.port.dto.result.InventoryReceiptResult;
import com.benhsoan.port.dto.result.InventoryReceiptWarningResult;
import com.benhsoan.port.inbound.inventory.ReceiveStockUseCase;
import com.benhsoan.port.outbound.repository.inventory.InventoryReceiptRepository;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ReceiveStockService implements ReceiveStockUseCase {

    static final String MERGED_WITH_EXISTING_BATCH_WARNING_CODE = "MERGED_WITH_EXISTING_BATCH";

    private final MedicineRepository medicineRepository;
    private final MedicineBatchRepository medicineBatchRepository;
    private final InventoryReceiptRepository inventoryReceiptRepository;
    private final InventoryManagementAuthorizer authorizer;
    private final InventoryReceiptResultMapper resultMapper;
    private final EligibleStockSnapshotService eligibleStockSnapshotService;
    private final LowStockAlertTransitionService lowStockAlertTransitionService;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private static final String BATCH_EXPIRY_MISMATCH_MESSAGE =
            "Batch number must map to a single expiry date for the same medicine.";

    @Override
    public InventoryReceiptResult receiveStock(ReceiveStockCommand command) {
        authorizer.requirePharmacistOrAdmin();
        requireCommand(command);

        Instant now = clockPort.now();
        UUID receivedBy = currentUserPort.getCurrentUserId();
        UUID receiptId = UUID.randomUUID();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        List<UUID> medicineIds = command.items().stream()
                .map(ReceiveStockItemCommand::medicineId)
                .distinct()
                .toList();

        List<MedicineBatch> batches = new ArrayList<>();
        List<InventoryReceiptItem> receiptItems = new ArrayList<>();
        List<InventoryReceiptWarningResult> warnings = new ArrayList<>();
        Map<UUID, Integer> beforeEligibleQuantities = new HashMap<>(
                eligibleStockSnapshotService.snapshotEligibleStockQuantities(medicineIds, today)
        );
        Map<UUID, List<MedicineBatch>> trackedBatchesByMedicine = new HashMap<>();

        for (ReceiveStockItemCommand itemCommand : command.items()) {
            validateItem(itemCommand, now);

            UUID itemId = UUID.randomUUID();
            UUID medicineId = itemCommand.medicineId();
            List<MedicineBatch> trackedBatches = trackedBatchesByMedicine.computeIfAbsent(
                    medicineId,
                    id -> new ArrayList<>(medicineBatchRepository.findByMedicineId(id))
            );

            BatchResolution batchResolution = findOrCreateBatch(itemCommand, now, trackedBatches);
            MedicineBatch batch = batchResolution.batch();
            batches.add(batch);
            if (batchResolution.mergedIntoExistingBatch()) {
                warnings.add(new InventoryReceiptWarningResult(
                        MERGED_WITH_EXISTING_BATCH_WARNING_CODE,
                        itemCommand.medicineId(),
                        itemCommand.batchNumber().trim(),
                        "Stock was merged into an existing batch with the same batch number and expiry date."
                ));
            }

            UUID batchId = batch.getId();

            InventoryReceiptItem item = InventoryReceiptItem.create(
                    itemId,
                    receiptId,
                    medicineId,
                    batchId,
                    itemCommand.quantity(),
                    itemCommand.importPrice(),
                    now
            );
            receiptItems.add(item);

            medicineRepository.updateStockQuantity(medicineId, itemCommand.quantity());
        }

        InventoryReceipt receipt = InventoryReceipt.create(
                receiptId,
                receivedBy,
                now,
                command.note(),
                now,
                receiptItems
        );

        inventoryReceiptRepository.save(receipt);
        lowStockAlertTransitionService.handleEligibleStockTransitions(
                medicineIds,
                beforeEligibleQuantities,
                today,
                now
        );

        return resultMapper.toResult(receipt, batches, warnings);
    }

    private void validateItem(ReceiveStockItemCommand item, Instant now) {
        if (item.medicineId() == null) {
            throw new ValidationException("Medicine id is required for each receipt item.");
        }
        if (item.batchNumber() == null || item.batchNumber().isBlank()) {
            throw new ValidationException("Batch number is required for each receipt item.");
        }
        if (item.quantity() <= 0) {
            throw new ValidationException("Quantity must be greater than 0.");
        }
        if (item.importPrice() == null || item.importPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new ValidationException("Import price must be non-negative.");
        }
        if (item.expiryDate() == null) {
            throw new ValidationException("Expiry date is required for each receipt item.");
        }
        LocalDate today = LocalDate.ofInstant(now, java.time.ZoneOffset.UTC);
        if (!item.expiryDate().isAfter(today)) {
            throw new ValidationException("Expiry date must be in the future.");
        }

        medicineRepository.findById(item.medicineId())
                .orElseThrow(() -> new ValidationException(
                        "Medicine not found with id: " + item.medicineId()));
    }

    private BatchResolution findOrCreateBatch(
            ReceiveStockItemCommand item,
            Instant now,
            List<MedicineBatch> trackedBatches
    ) {
        return medicineBatchRepository
                .findByMedicineIdAndBatchNumber(item.medicineId(), item.batchNumber().trim())
                .map(existingBatch -> {
                    validateBatchExpiryConsistency(existingBatch, item);
                    existingBatch.addStock(item.quantity(), now);
                    medicineBatchRepository.addStockQuantity(existingBatch.getId(), item.quantity());
                    return new BatchResolution(existingBatch, true);
                })
                .orElseGet(() -> {
                    MedicineBatch newBatch = MedicineBatch.create(
                            UUID.randomUUID(),
                            item.medicineId(),
                            item.batchNumber().trim(),
                            item.expiryDate(),
                            now
                    );
                    newBatch.addStock(item.quantity(), now);
                    MedicineBatch savedBatch = medicineBatchRepository.save(newBatch);
                    trackedBatches.add(savedBatch);
                    return new BatchResolution(savedBatch, false);
                });
    }

    private void validateBatchExpiryConsistency(MedicineBatch existingBatch, ReceiveStockItemCommand item) {
        if (existingBatch.getExpiryDate().equals(item.expiryDate())) {
            return;
        }

        throw new ValidationException(
                BATCH_EXPIRY_MISMATCH_MESSAGE
                        + " medicineId=" + item.medicineId()
                        + ", batchNumber=" + item.batchNumber().trim()
                        + ", existingExpiryDate=" + existingBatch.getExpiryDate()
                        + ", requestedExpiryDate=" + item.expiryDate()
        );
    }

    private static void requireCommand(ReceiveStockCommand command) {
        if (command == null) {
            throw new ValidationException("Receive stock command is required.");
        }
        if (command.items() == null || command.items().isEmpty()) {
            throw new ValidationException("At least one receipt item is required.");
        }
        checkForDuplicateItems(command.items());
    }

    private static void checkForDuplicateItems(List<ReceiveStockItemCommand> items) {
        Set<String> seen = new HashSet<>();
        for (ReceiveStockItemCommand item : items) {
            if (item.medicineId() != null && item.batchNumber() != null) {
                String key = item.medicineId() + ":" + item.batchNumber().trim();
                if (!seen.add(key)) {
                    throw new ValidationException(
                            "Duplicate item detected: medicineId=" + item.medicineId()
                                    + ", batchNumber=" + item.batchNumber().trim());
                }
            }
        }
    }

    private record BatchResolution(
            MedicineBatch batch,
            boolean mergedIntoExistingBatch
    ) {
    }
}
