package com.benhsoan.application.ucservice.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.port.dto.result.InventoryStockReportItemResult;
import com.benhsoan.port.dto.result.InventoryStockReportResult;
import com.benhsoan.port.inbound.inventory.GetInventoryStockReportUseCase;
import com.benhsoan.port.outbound.repository.inventory.InventoryStockMovementSummary;
import com.benhsoan.port.outbound.repository.inventory.InventoryStockReportRepository;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetInventoryStockReportService implements GetInventoryStockReportUseCase {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final InventoryStockReportRepository reportRepository;
    private final MedicineRepository medicineRepository;
    private final ClockPort clockPort;

    @Override
    public InventoryStockReportResult getStockReport(LocalDate from, LocalDate to) {
        Instant fromInclusive = from.atStartOfDay(CLINIC_ZONE).toInstant();
        Instant toExclusive = to.plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant();

        List<InventoryStockMovementSummary> summaries = reportRepository
                .summarizeMovements(fromInclusive, toExclusive)
                .stream()
                .filter(GetInventoryStockReportService::hasMeaningfulData)
                .toList();

        Map<UUID, Medicine> medicinesById = medicineRepository
                .findAllById(summaries.stream()
                        .map(InventoryStockMovementSummary::medicineId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(Medicine::getId, Function.identity()));

        List<InventoryStockReportItemResult> items = summaries.stream()
                .map(summary -> toItem(summary, medicinesById.get(summary.medicineId())))
                .sorted(Comparator.comparing(InventoryStockReportItemResult::medicineCode,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        boolean hasTransactions = summaries.stream().anyMatch(summary ->
                summary.receivedQuantity() != 0
                        || summary.dispensedQuantity() != 0
                        || summary.returnedQuantity() != 0
                        || summary.adjustedQuantity() != 0);

        return new InventoryStockReportResult(from, to, clockPort.now(), hasTransactions, items);
    }

    private static boolean hasMeaningfulData(InventoryStockMovementSummary summary) {
        return summary.openingQuantity() != 0
                || summary.receivedQuantity() != 0
                || summary.dispensedQuantity() != 0
                || summary.returnedQuantity() != 0
                || summary.adjustedQuantity() != 0;
    }

    private InventoryStockReportItemResult toItem(InventoryStockMovementSummary summary, Medicine medicine) {
        int closingQuantity = summary.openingQuantity()
                + summary.receivedQuantity()
                - summary.dispensedQuantity()
                + summary.returnedQuantity()
                + summary.adjustedQuantity();

        return new InventoryStockReportItemResult(
                summary.medicineId(),
                medicine != null ? medicine.getMedicineCode() : null,
                medicine != null ? medicine.getMedicineName() : null,
                medicine != null ? medicine.getUnit() : null,
                summary.openingQuantity(),
                summary.receivedQuantity(),
                summary.dispensedQuantity(),
                summary.returnedQuantity(),
                summary.adjustedQuantity(),
                closingQuantity
        );
    }
}
