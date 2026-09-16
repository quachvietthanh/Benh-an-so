package com.benhsoan.application.ucservice.prescription;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.auth.User;
import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.prescription.PrescriptionDispenseItem;
import com.benhsoan.domain.prescription.exception.PrescriptionNotFoundException;
import com.benhsoan.port.dto.result.PrescriptionDispenseHistoryResult;
import com.benhsoan.port.inbound.prescription.GetPrescriptionDispenseHistoryUseCase;
import com.benhsoan.port.outbound.repository.auth.UserRepository;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionDispenseItemRepository;
import com.benhsoan.port.outbound.repository.prescription.PrescriptionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPrescriptionDispenseHistoryService implements GetPrescriptionDispenseHistoryUseCase {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionDispenseItemRepository dispenseItemRepository;
    private final PrescriptionReadAccessValidator accessValidator;
    private final MedicineRepository medicineRepository;
    private final MedicineBatchRepository medicineBatchRepository;
    private final UserRepository userRepository;

    @Override
    public List<PrescriptionDispenseHistoryResult> getHistory(UUID prescriptionId) {
        if (prescriptionId == null) {
            return List.of();
        }
        var prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new PrescriptionNotFoundException(prescriptionId));
        accessValidator.requireCanReadDispenseHistory(prescription);

        List<PrescriptionDispenseItem> items = dispenseItemRepository.findByPrescriptionId(prescriptionId);
        if (items.isEmpty()) {
            return List.of();
        }

        Map<UUID, Medicine> medicines = medicineRepository.findAllById(
                        items.stream().map(PrescriptionDispenseItem::getMedicineId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Medicine::getId, Function.identity()));

        Map<UUID, MedicineBatch> batches = medicineBatchRepository.findAllById(
                        items.stream().map(PrescriptionDispenseItem::getMedicineBatchId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(MedicineBatch::getId, Function.identity()));

        Map<UUID, User> users = userRepository.findAllById(
                        items.stream().map(PrescriptionDispenseItem::getDispensedBy).distinct().toList())
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return items.stream()
                .map(item -> toResult(item, medicines, batches, users))
                .toList();
    }

    private PrescriptionDispenseHistoryResult toResult(
            PrescriptionDispenseItem item,
            Map<UUID, Medicine> medicines,
            Map<UUID, MedicineBatch> batches,
            Map<UUID, User> users
    ) {
        Medicine medicine = medicines.get(item.getMedicineId());
        MedicineBatch batch = batches.get(item.getMedicineBatchId());
        User dispenser = users.get(item.getDispensedBy());
        return new PrescriptionDispenseHistoryResult(
                item.getId(),
                item.getPrescriptionId(),
                item.getPrescriptionItemId(),
                item.getMedicineId(),
                item.getMedicineBatchId(),
                item.getDispensedQuantity(),
                item.getDispensedBy(),
                item.getDispensedAt(),
                medicine != null ? medicine.getMedicineName() : null,
                batch != null ? batch.getBatchNumber() : null,
                dispenser != null ? dispenser.getFullName() : null);
    }
}
