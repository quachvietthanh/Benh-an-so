package com.benhsoan.application.ucservice.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.inventory.enums.StockMovementType;
import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.query.inventory.GetInventoryInOutStockReportQuery;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockItemResult;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockReportResult;
import com.benhsoan.port.dto.result.inventory.InventoryInOutStockSummaryResult;
import com.benhsoan.port.dto.result.inventory.MedicineMovementSummaryResult;
import com.benhsoan.port.dto.result.inventory.MedicineStockQuantityResult;
import com.benhsoan.port.inbound.inventory.GetInventoryInOutStockReportUseCase;
import com.benhsoan.port.outbound.repository.inventory.StockMovementRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineSearchCriteria;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetInventoryInOutStockReportService implements GetInventoryInOutStockReportUseCase {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final long MAX_REPORT_RANGE_DAYS = 366;

    private final StockMovementRepository stockMovementRepository;
    private final MedicineRepository medicineRepository;
    private final InventoryReportAuthorizer authorizer;
    private final ClockPort clockPort;

    @Override
    public InventoryInOutStockReportResult getReport(GetInventoryInOutStockReportQuery query) {
        authorizer.requireReportAccess();
        validateQuery(query);

        Instant startInstant = query.from().atStartOfDay(CLINIC_ZONE).toInstant();
        Instant endInstant = query.to().plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant();
        Instant now = clockPort.now();

        List<Medicine> medicines = resolveMedicines(query);

        List<MedicineStockQuantityResult> openingStockResults = query.medicineId() != null
                ? stockMovementRepository.sumQuantitiesBeforeForMedicine(query.medicineId(), startInstant)
                : stockMovementRepository.sumQuantitiesBefore(startInstant);

        List<MedicineMovementSummaryResult> periodMovementResults = query.medicineId() != null
                ? stockMovementRepository.sumMovementsBetweenForMedicine(query.medicineId(), startInstant, endInstant)
                : stockMovementRepository.sumMovementsBetween(startInstant, endInstant);

        Map<UUID, Integer> openingStockByMedicine = openingStockResults.stream()
                .collect(Collectors.toMap(
                        MedicineStockQuantityResult::medicineId,
                        r -> (int) r.totalQuantity(),
                        (existing, replacement) -> existing
                ));

        Map<UUID, Map<StockMovementType, Integer>> movementsByMedicineAndType = new HashMap<>();
        for (MedicineMovementSummaryResult movement : periodMovementResults) {
            movementsByMedicineAndType
                    .computeIfAbsent(movement.medicineId(), k -> new EnumMap<>(StockMovementType.class))
                    .put(movement.movementType(), (int) movement.totalQuantityChange());
        }

        boolean hasTransactions = false;
        int totalOpeningStock = 0;
        int totalImportQuantity = 0;
        int totalDispensedQuantity = 0;
        int totalReturnedQuantity = 0;
        int totalAdjustedQuantity = 0;
        int totalClosingStock = 0;

        List<InventoryInOutStockItemResult> items = new ArrayList<>();
        for (Medicine med : medicines) {
            int openingStock = openingStockByMedicine.getOrDefault(med.getId(), 0);
            Map<StockMovementType, Integer> typeMap = movementsByMedicineAndType
                    .getOrDefault(med.getId(), Collections.emptyMap());

            int importQuantity = typeMap.getOrDefault(StockMovementType.RECEIPT, 0);
            int rawDispensed = typeMap.getOrDefault(StockMovementType.DISPENSE, 0);
            int dispensedQuantity = Math.abs(rawDispensed);
            int returnedQuantity = typeMap.getOrDefault(StockMovementType.RETURN, 0);
            int adjustmentPart = typeMap.getOrDefault(StockMovementType.ADJUSTMENT, 0);
            int expirePart = typeMap.getOrDefault(StockMovementType.EXPIRE, 0);
            int adjustedQuantity = adjustmentPart + expirePart;

            int closingStock = openingStock + importQuantity - dispensedQuantity + returnedQuantity + adjustedQuantity;

            if (importQuantity != 0 || dispensedQuantity != 0 || returnedQuantity != 0 || adjustedQuantity != 0) {
                hasTransactions = true;
            }

            totalOpeningStock += openingStock;
            totalImportQuantity += importQuantity;
            totalDispensedQuantity += dispensedQuantity;
            totalReturnedQuantity += returnedQuantity;
            totalAdjustedQuantity += adjustedQuantity;
            totalClosingStock += closingStock;

            items.add(new InventoryInOutStockItemResult(
                    med.getId(),
                    med.getMedicineCode(),
                    med.getMedicineName(),
                    med.getUnit(),
                    openingStock,
                    importQuantity,
                    dispensedQuantity,
                    returnedQuantity,
                    adjustedQuantity,
                    closingStock
            ));
        }

        InventoryInOutStockSummaryResult summary = new InventoryInOutStockSummaryResult(
                medicines.size(),
                totalOpeningStock,
                totalImportQuantity,
                totalDispensedQuantity,
                totalReturnedQuantity,
                totalAdjustedQuantity,
                totalClosingStock
        );

        return new InventoryInOutStockReportResult(
                query.from(),
                query.to(),
                now,
                hasTransactions,
                items,
                summary
        );
    }

    private void validateQuery(GetInventoryInOutStockReportQuery query) {
        if (query == null) {
            throw new ValidationException("Query must not be null.");
        }
        if (query.from() == null) {
            throw new ValidationException("from date is required.");
        }
        if (query.to() == null) {
            throw new ValidationException("to date is required.");
        }
        if (query.from().isAfter(query.to())) {
            throw new ValidationException("from must be before or equal to to.");
        }
        long days = ChronoUnit.DAYS.between(query.from(), query.to()) + 1;
        if (days > MAX_REPORT_RANGE_DAYS) {
            throw new ValidationException("Date range must not exceed " + MAX_REPORT_RANGE_DAYS + " days.");
        }
    }

    private List<Medicine> resolveMedicines(GetInventoryInOutStockReportQuery query) {
        if (query.medicineId() != null) {
            Medicine medicine = medicineRepository.findById(query.medicineId())
                    .orElseThrow(() -> new ValidationException("Medicine not found with id: " + query.medicineId()));
            return List.of(medicine);
        }

        List<Medicine> medicines;
        if (query.keyword() != null && !query.keyword().isBlank()) {
            MedicineSearchCriteria criteria = new MedicineSearchCriteria(query.keyword().trim(), null, null, null);
            medicines = new ArrayList<>(medicineRepository.search(criteria, Pageable.unpaged()).getContent());
        } else {
            medicines = new ArrayList<>(medicineRepository.findAll());
        }

        medicines.sort(Comparator.comparing(Medicine::getMedicineCode, Comparator.nullsLast(String::compareToIgnoreCase)));
        return medicines;
    }
}
