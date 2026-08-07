package com.benhsoan.application.ucservice.prescription;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.inventory.StockMovement;
import com.benhsoan.domain.inventory.enums.StockMovementReferenceType;
import com.benhsoan.domain.inventory.enums.StockMovementType;
import com.benhsoan.domain.medicine.exception.MedicineNotFoundException;
import com.benhsoan.domain.prescription.PrescriptionDispenseItem;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.exception.PrescriptionInsufficientStockException;
import com.benhsoan.domain.prescription.exception.PrescriptionNotFoundException;
import com.benhsoan.port.dto.result.DispenseAllocationResult;
import com.benhsoan.port.dto.result.DispensePrescriptionResult;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.inventory.StockMovementRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionDispenseItemRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionItemRepository;
import com.benhsoan.port.inbound.prescription.DispensePrescriptionUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionWarningLogRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DispensePrescriptionService implements DispensePrescriptionUseCase {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final PrescriptionDispenseItemRepository prescriptionDispenseItemRepository;
    private final PrescriptionWarningLogRepository warningLogRepository;
    private final MedicineRepository medicineRepository;
    private final MedicineBatchRepository medicineBatchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;
    private final AuditLogRepository auditLogRepository;
    private final DispensePrescriptionResultMapper resultMapper;

    @Override
    public DispensePrescriptionResult dispense(UUID prescriptionId) {
        if (!currentUserPort.hasRole("PHARMACIST")
                && !currentUserPort.hasRole("ADMIN")) {
            throw new AccessDeniedException("Only pharmacists can dispense prescriptions.");
        }
        UUID actorId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        var prescription = prescriptionRepository.findByIdForUpdate(prescriptionId)
                .orElseThrow(() -> new PrescriptionNotFoundException(prescriptionId));
        List<PrescriptionItem> prescriptionItems = prescriptionItemRepository.findByPrescriptionId(prescriptionId);

        AllocationComputation computation = computeAllocations(
                prescription.getId(),
                prescriptionItems,
                now,
                today
        );

        List<DispenseAllocationResult> allocations = applyAllocations(computation, actorId, now);
        prescription.markDispensed(actorId, now);
        var saved = prescriptionRepository.save(prescription);
        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.DISPENSE,
                ResourceType.PRESCRIPTION,
                saved.getId(),
                "{\"prescriptionCode\":\"%s\"}".formatted(saved.getPrescriptionCode()),
                null
        ));
        return resultMapper.toResult(
                saved,
                warningLogRepository.findByPrescriptionId(saved.getId()),
                actorId,
                now,
                allocations
        );
    }

    private AllocationComputation computeAllocations(
            UUID prescriptionId,
            List<PrescriptionItem> prescriptionItems,
            Instant now,
            LocalDate today
    ) {
        List<AllocationPlan> allocationPlans = new ArrayList<>();
        List<PrescriptionInsufficientStockException.StockShortageDetail> shortages = new ArrayList<>();

        for (PrescriptionItem item : prescriptionItems) {
            var medicine = medicineRepository.findById(item.getMedicineId())
                    .orElseThrow(() -> new MedicineNotFoundException(item.getMedicineId()));

            List<MedicineBatch> availableBatches = medicineBatchRepository
                    .findAvailableByMedicineIdForUpdate(item.getMedicineId(), today);

            int availableQuantity = availableBatches.stream()
                    .mapToInt(MedicineBatch::getQuantity)
                    .sum();

            if (availableQuantity < item.getQuantity()) {
                shortages.add(new PrescriptionInsufficientStockException.StockShortageDetail(
                        item.getId(),
                        item.getMedicineId(),
                        medicine.getMedicineCode(),
                        item.getMedicineName(),
                        item.getQuantity(),
                        availableQuantity,
                        item.getQuantity() - availableQuantity
                ));
                continue;
            }

            int remaining = item.getQuantity();
            for (MedicineBatch batch : availableBatches) {
                if (remaining == 0) {
                    break;
                }
                int allocatedQuantity = Math.min(remaining, batch.getQuantity());
                allocationPlans.add(new AllocationPlan(
                        item,
                        batch,
                        medicine.getMedicineCode(),
                        allocatedQuantity
                ));
                remaining -= allocatedQuantity;
            }
        }

        if (!shortages.isEmpty()) {
            throw new PrescriptionInsufficientStockException(prescriptionId, shortages);
        }

        return new AllocationComputation(allocationPlans);
    }

    private List<DispenseAllocationResult> applyAllocations(
            AllocationComputation computation,
            UUID actorId,
            Instant now
    ) {
        List<PrescriptionDispenseItem> dispenseItems = new ArrayList<>();
        List<StockMovement> stockMovements = new ArrayList<>();
        Map<UUID, Integer> medicineDeltas = new HashMap<>();
        List<DispenseAllocationResult> allocations = new ArrayList<>();

        for (AllocationPlan plan : computation.allocationPlans()) {
            MedicineBatch batch = plan.batch();
            int quantityBefore = batch.getQuantity();
            batch.deductStock(plan.allocatedQuantity(), now);
            medicineBatchRepository.deductStockQuantity(
                    batch.getId(),
                    plan.allocatedQuantity(),
                    batch.getStatus(),
                    now
            );

            dispenseItems.add(PrescriptionDispenseItem.create(
                    UUID.randomUUID(),
                    plan.item().getPrescriptionId(),
                    plan.item().getId(),
                    plan.item().getMedicineId(),
                    batch.getId(),
                    plan.allocatedQuantity(),
                    actorId,
                    now
            ));

            stockMovements.add(StockMovement.create(
                    UUID.randomUUID(),
                    plan.item().getMedicineId(),
                    batch.getId(),
                    StockMovementType.DISPENSE,
                    StockMovementReferenceType.PRESCRIPTION_ITEM,
                    plan.item().getId(),
                    -plan.allocatedQuantity(),
                    quantityBefore,
                    batch.getQuantity(),
                    actorId,
                    now,
                    "Dispensed for prescription item " + plan.item().getId()
            ));

            medicineDeltas.merge(plan.item().getMedicineId(), -plan.allocatedQuantity(), Integer::sum);

            allocations.add(new DispenseAllocationResult(
                    dispenseItems.get(dispenseItems.size() - 1).getId(),
                    plan.item().getId(),
                    plan.item().getMedicineId(),
                    plan.medicineCode(),
                    plan.item().getMedicineName(),
                    batch.getId(),
                    batch.getBatchNumber(),
                    batch.getExpiryDate(),
                    plan.allocatedQuantity(),
                    batch.getQuantity()
            ));
        }

        prescriptionDispenseItemRepository.saveAll(dispenseItems);
        stockMovementRepository.saveAll(stockMovements);
        medicineDeltas.forEach(medicineRepository::updateStockQuantity);
        return allocations;
    }

    private record AllocationPlan(
            PrescriptionItem item,
            MedicineBatch batch,
            String medicineCode,
            int allocatedQuantity
    ) {
    }

    private record AllocationComputation(
            List<AllocationPlan> allocationPlans
    ) {
    }
}
