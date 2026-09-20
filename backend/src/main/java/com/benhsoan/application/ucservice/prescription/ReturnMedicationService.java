package com.benhsoan.application.ucservice.prescription;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auditlog.AuditLog;
import com.benhsoan.domain.auditlog.enums.ActionType;
import com.benhsoan.domain.auditlog.enums.ResourceType;
import com.benhsoan.domain.billing.Payment;
import com.benhsoan.domain.billing.enums.PaymentStatus;
import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.inventory.StockMovement;
import com.benhsoan.domain.inventory.enums.BatchStatus;
import com.benhsoan.domain.inventory.enums.StockMovementReferenceType;
import com.benhsoan.domain.inventory.enums.StockMovementType;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.medicalrecord.MedicalRecord;
import com.benhsoan.domain.prescription.MedicationReturn;
import com.benhsoan.domain.prescription.Prescription;
import com.benhsoan.domain.prescription.PrescriptionDispenseItem;
import com.benhsoan.domain.prescription.PrescriptionItem;
import com.benhsoan.domain.prescription.enums.PrescriptionStatus;
import com.benhsoan.domain.prescription.exception.PrescriptionNotFoundException;
import com.benhsoan.domain.prescription.exception.PrescriptionReturnPaymentNotRefundedException;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.command.prescription.ReturnMedicationCommand;
import com.benhsoan.port.dto.command.prescription.ReturnMedicationItemCommand;
import com.benhsoan.port.dto.result.ReturnMedicationResult;
import com.benhsoan.port.dto.result.ReturnedMedicationItemResult;
import com.benhsoan.port.inbound.prescription.ReturnMedicationUseCase;
import com.benhsoan.port.outbound.repository.audit.AuditLogRepository;
import com.benhsoan.port.outbound.repository.billing.PaymentRepository;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.inventory.StockMovementRepository;
import com.benhsoan.port.outbound.repository.medicalrecord.MedicalRecordRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.prescription.MedicationReturnRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionDispenseItemRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;
import com.benhsoan.port.outbound.security.CurrentUserPort;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ReturnMedicationService implements ReturnMedicationUseCase {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionDispenseItemRepository dispenseItemRepository;
    private final MedicationReturnRepository medicationReturnRepository;
    private final MedicineBatchRepository medicineBatchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final MedicineRepository medicineRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PaymentRepository paymentRepository;
    private final AuditLogRepository auditLogRepository;
    private final CurrentUserPort currentUserPort;
    private final ClockPort clockPort;

    @Override
    public ReturnMedicationResult returnMedication(ReturnMedicationCommand command) {
        requireCommand(command);
        ensureAuthorized();

        UUID actorId = currentUserPort.getCurrentUserId();
        Instant now = clockPort.now();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);

        Prescription prescription = prescriptionRepository.findByIdForUpdate(command.prescriptionId())
                .orElseThrow(() -> new PrescriptionNotFoundException(command.prescriptionId()));

        ensureReturnable(prescription);
        requirePaymentRefundedOrUnpaid(prescription.getMedicalRecordId());

        String reason = command.reason().trim();
        List<PrescriptionDispenseItem> dispenseItems = dispenseItemRepository
                .findByPrescriptionId(prescription.getId());
        Map<UUID, PrescriptionDispenseItem> dispenseItemById = dispenseItems.stream()
                .collect(Collectors.toMap(PrescriptionDispenseItem::getId, Function.identity()));

        List<Medicine> medicines = medicineRepository.findAllById(
                dispenseItems.stream().map(PrescriptionDispenseItem::getMedicineId).distinct().toList());
        Map<UUID, Medicine> medicineById = medicines.stream()
                .collect(Collectors.toMap(Medicine::getId, Function.identity()));

        List<MedicineBatch> batches = medicineBatchRepository.findAllById(
                dispenseItems.stream().map(PrescriptionDispenseItem::getMedicineBatchId).distinct().toList());
        Map<UUID, MedicineBatch> batchById = batches.stream()
                .collect(Collectors.toMap(MedicineBatch::getId, Function.identity()));

        List<MedicationReturn> returns = new ArrayList<>();
        List<StockMovement> stockMovements = new ArrayList<>();
        Map<UUID, Integer> returnedByPrescriptionItem = new HashMap<>();
        List<ReturnedMedicationItemResult> returnResults = new ArrayList<>();

        for (ReturnMedicationItemCommand itemCommand : command.items()) {
            PrescriptionDispenseItem dispenseItem = dispenseItemById.get(itemCommand.dispenseItemId());
            if (dispenseItem == null) {
                throw new ValidationException(
                        "Dispense item not found for this prescription: " + itemCommand.dispenseItemId());
            }
            if (itemCommand.quantity() <= 0) {
                throw new ValidationException("Returned quantity must be greater than zero.");
            }

            MedicineBatch batch = batchById.get(dispenseItem.getMedicineBatchId());
            if (batch == null) {
                throw new ValidationException(
                        "Medicine batch not found: " + dispenseItem.getMedicineBatchId());
            }

            dispenseItem.recordReturn(itemCommand.quantity());

            int quantityBefore = batch.getQuantity();
            BatchStatus targetStatus = batch.getExpiryDate().isBefore(today)
                    ? BatchStatus.EXPIRED
                    : BatchStatus.ACTIVE;
            medicineBatchRepository.restoreStockQuantity(
                    batch.getId(), itemCommand.quantity(), targetStatus, now);

            UUID returnId = UUID.randomUUID();
            returns.add(MedicationReturn.create(
                    returnId,
                    prescription.getId(),
                    dispenseItem.getPrescriptionItemId(),
                    dispenseItem.getId(),
                    dispenseItem.getMedicineId(),
                    batch.getId(),
                    itemCommand.quantity(),
                    reason,
                    actorId,
                    now));

            stockMovements.add(StockMovement.create(
                    UUID.randomUUID(),
                    dispenseItem.getMedicineId(),
                    batch.getId(),
                    StockMovementType.RETURN,
                    StockMovementReferenceType.RETURN,
                    returnId,
                    itemCommand.quantity(),
                    quantityBefore,
                    quantityBefore + itemCommand.quantity(),
                    actorId,
                    now,
                    reason));

            returnedByPrescriptionItem.merge(
                    dispenseItem.getPrescriptionItemId(), itemCommand.quantity(), Integer::sum);

            Medicine medicine = medicineById.get(dispenseItem.getMedicineId());
            returnResults.add(new ReturnedMedicationItemResult(
                    returnId,
                    dispenseItem.getId(),
                    dispenseItem.getPrescriptionItemId(),
                    dispenseItem.getMedicineId(),
                    medicine != null ? medicine.getMedicineName() : null,
                    batch.getId(),
                    batch.getBatchNumber(),
                    itemCommand.quantity(),
                    dispenseItem.getRemainingReturnableQuantity()));
        }


        for (PrescriptionItem item : prescription.getItems()) {
            Integer returned = returnedByPrescriptionItem.get(item.getId());
            if (returned != null) {
                item.recordReturn(returned);
            }
        }
        prescription.recomputeStatusAfterReturn(actorId, now);

        Prescription saved = prescriptionRepository.save(prescription);
        dispenseItemRepository.saveAll(dispenseItems);
        returns.forEach(medicationReturnRepository::save);
        stockMovementRepository.saveAll(stockMovements);

        auditLogRepository.save(AuditLog.create(
                actorId,
                ActionType.RETURN,
                ResourceType.PRESCRIPTION,
                saved.getId(),
                "{\"prescriptionCode\":\"%s\",\"status\":\"%s\"}".formatted(
                        saved.getPrescriptionCode(), saved.getStatus()),
                null,
                now));

        return new ReturnMedicationResult(
                saved.getId(),
                saved.getStatus(),
                actorId,
                now,
                List.copyOf(returnResults));
    }

    private void ensureAuthorized() {
        if (!currentUserPort.hasRole("PHARMACIST") && !currentUserPort.hasRole("ADMIN")) {
            throw new AccessDeniedException("Only pharmacists can return medication.");
        }
    }

    private void ensureReturnable(Prescription prescription) {
        PrescriptionStatus status = prescription.getStatus();
        if (status != PrescriptionStatus.DISPENSED && status != PrescriptionStatus.PARTIALLY_DISPENSED) {
            throw new ValidationException(
                    "Only dispensed or partially dispensed prescriptions can accept medication returns.");
        }
    }

    private void requirePaymentRefundedOrUnpaid(UUID medicalRecordId) {
        MedicalRecord medicalRecord = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new ValidationException("Medical record not found: " + medicalRecordId));

        Payment payment = paymentRepository.findByVisitId(medicalRecord.getVisitId()).orElse(null);
        if (payment != null
                && (payment.getStatus() == PaymentStatus.RECORDED
                        || payment.getStatus() == PaymentStatus.SUCCESS)) {
            throw new PrescriptionReturnPaymentNotRefundedException();
        }
    }

    private void requireCommand(ReturnMedicationCommand command) {
        if (command == null || command.prescriptionId() == null) {
            throw new ValidationException("Prescription id is required.");
        }
        if (command.reason() == null || command.reason().isBlank()) {
            throw new ValidationException("Return reason is required.");
        }
        if (command.items() == null || command.items().isEmpty()) {
            throw new ValidationException("At least one return item is required.");
        }
    }
}

