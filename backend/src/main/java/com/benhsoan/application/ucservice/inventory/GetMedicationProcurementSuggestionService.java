package com.benhsoan.application.ucservice.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.port.dto.result.ProcurementSuggestionItemResult;
import com.benhsoan.port.dto.result.ProcurementSuggestionResult;
import com.benhsoan.port.inbound.inventory.GetMedicationProcurementSuggestionUseCase;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.reporting.OperationalReportQueryRepository;
import com.benhsoan.port.outbound.repository.reporting.TopMedicineSummary;
import com.benhsoan.port.outbound.time.ClockPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMedicationProcurementSuggestionService implements GetMedicationProcurementSuggestionUseCase {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final MedicineRepository medicineRepository;
    private final EligibleStockSnapshotService eligibleStockSnapshotService;
    private final OperationalReportQueryRepository operationalReportQueryRepository;
    private final MedicationProcurementSuggestionCalculator calculator;
    private final ClockPort clockPort;

    @Override
    public ProcurementSuggestionResult getSuggestions(LocalDate from, LocalDate to, boolean onlyBelowThreshold) {
        Instant now = clockPort.now();
        LocalDate today = LocalDate.ofInstant(now, CLINIC_ZONE);

        LocalDate effectiveTo = (to != null) ? to : today;
        LocalDate effectiveFrom = (from != null) ? from : effectiveTo.minusDays(30);

        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new ValidationException("Ngày bắt đầu kỳ tham chiếu không được sau ngày kết thúc.");
        }

        Instant fromInclusive = effectiveFrom.atStartOfDay(CLINIC_ZONE).toInstant();
        Instant toExclusive = effectiveTo.plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant();

        List<Medicine> medicines = medicineRepository.findAllActive();
        if (medicines.isEmpty()) {
            return new ProcurementSuggestionResult(effectiveFrom, effectiveTo, now, 0, List.of());
        }

        List<UUID> medicineIds = medicines.stream().map(Medicine::getId).toList();
        Map<UUID, Integer> eligibleStockMap =
                eligibleStockSnapshotService.snapshotEligibleStockQuantities(medicineIds, today);

        Map<UUID, Long> consumptionMap = operationalReportQueryRepository
                .findTopDispensedMedicines(fromInclusive, toExclusive)
                .stream()
                .collect(Collectors.toMap(
                        TopMedicineSummary::medicineId,
                        TopMedicineSummary::totalDispensedQuantity,
                        Long::sum
                ));

        List<ProcurementSuggestionItemResult> suggestionItems = new ArrayList<>();

        for (Medicine medicine : medicines) {
            int eligibleStock = eligibleStockMap.getOrDefault(medicine.getId(), 0);
            int minThreshold = medicine.getMinStockThreshold();
            long consumption = consumptionMap.getOrDefault(medicine.getId(), 0L);

            int suggestedQuantity = calculator.calculateSuggestedQuantity(eligibleStock, minThreshold, consumption);

            if (calculator.shouldIncludeInSuggestion(eligibleStock, minThreshold, suggestedQuantity, onlyBelowThreshold)) {
                suggestionItems.add(new ProcurementSuggestionItemResult(
                        medicine.getId(),
                        medicine.getMedicineCode(),
                        medicine.getMedicineName(),
                        medicine.getUnit(),
                        medicine.getStockQuantity(),
                        eligibleStock,
                        minThreshold,
                        (int) consumption,
                        suggestedQuantity
                ));
            }
        }

        return new ProcurementSuggestionResult(
                effectiveFrom,
                effectiveTo,
                now,
                suggestionItems.size(),
                suggestionItems
        );
    }
}
