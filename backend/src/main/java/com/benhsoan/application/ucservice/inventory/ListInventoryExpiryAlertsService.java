package com.benhsoan.application.ucservice.inventory;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.inventory.MedicineBatch;
import com.benhsoan.domain.inventory.enums.InventoryExpiryAlertStatus;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.port.dto.query.inventory.ListInventoryExpiryAlertsQuery;
import com.benhsoan.port.dto.result.InventoryExpiryAlertResult;
import com.benhsoan.port.inbound.inventory.ListInventoryExpiryAlertsUseCase;
import com.benhsoan.port.outbound.repository.inventory.MedicineBatchRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListInventoryExpiryAlertsService implements ListInventoryExpiryAlertsUseCase {

    private final MedicineBatchRepository medicineBatchRepository;
    private final MedicineRepository medicineRepository;
    private final InventoryManagementAuthorizer authorizer;
    private final ClockPort clockPort;

    @Value("${inventory.expiry-alert-days:30}")
    private long expiryAlertDays;

    @Override
    public List<InventoryExpiryAlertResult> list(ListInventoryExpiryAlertsQuery query) {
        authorizer.requirePharmacistOrAdmin();

        ListInventoryExpiryAlertsQuery effectiveQuery = query == null
                ? new ListInventoryExpiryAlertsQuery(null, InventoryExpiryAlertStatus.ALL)
                : query;
        InventoryExpiryAlertStatus status = effectiveQuery.status() == null
                ? InventoryExpiryAlertStatus.ALL
                : effectiveQuery.status();
        LocalDate today = LocalDate.ofInstant(clockPort.now(), ZoneOffset.UTC);

        List<MedicineBatch> batches = effectiveQuery.medicineId() == null
                ? medicineBatchRepository.findAll()
                : medicineBatchRepository.findByMedicineId(effectiveQuery.medicineId());

        Map<UUID, Medicine> medicinesById = medicineRepository.findAllById(
                        batches.stream()
                                .map(MedicineBatch::getMedicineId)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(Medicine::getId, Function.identity()));

        return batches.stream()
                .map(batch -> toAlertResultOrNull(batch, medicinesById.get(batch.getMedicineId()), today, status))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private InventoryExpiryAlertResult toAlertResultOrNull(
            MedicineBatch batch,
            Medicine medicine,
            LocalDate today,
            InventoryExpiryAlertStatus requestedStatus
    ) {
        InventoryExpiryAlertStatus alertStatus = resolveAlertStatus(batch, today);
        if (alertStatus == null) {
            return null;
        }
        if (requestedStatus != InventoryExpiryAlertStatus.ALL && requestedStatus != alertStatus) {
            return null;
        }

        return new InventoryExpiryAlertResult(
                batch.getId(),
                batch.getMedicineId(),
                medicine == null ? null : medicine.getMedicineCode(),
                medicine == null ? null : medicine.getMedicineName(),
                batch.getBatchNumber(),
                batch.getExpiryDate(),
                batch.getQuantity(),
                batch.getStatus(),
                ChronoUnit.DAYS.between(today, batch.getExpiryDate()),
                alertStatus,
                batch.getCreatedAt(),
                batch.getUpdatedAt()
        );
    }

    private InventoryExpiryAlertStatus resolveAlertStatus(MedicineBatch batch, LocalDate today) {
        if (batch.isExpiredOn(today)) {
            return InventoryExpiryAlertStatus.EXPIRED;
        }
        if (batch.isNearExpiryOn(today, expiryAlertDays)) {
            return InventoryExpiryAlertStatus.NEAR_EXPIRY;
        }
        return null;
    }
}
