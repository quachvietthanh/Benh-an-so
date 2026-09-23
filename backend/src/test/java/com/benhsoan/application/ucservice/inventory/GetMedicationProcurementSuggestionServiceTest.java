package com.benhsoan.application.ucservice.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.benhsoan.domain.medicine.Medicine;
import com.benhsoan.port.dto.result.ProcurementSuggestionResult;
import com.benhsoan.port.outbound.repository.medicine.MedicineRepository;
import com.benhsoan.port.outbound.repository.reporting.OperationalReportQueryRepository;
import com.benhsoan.port.outbound.repository.reporting.TopMedicineSummary;
import com.benhsoan.port.outbound.time.ClockPort;

@ExtendWith(MockitoExtension.class)
class GetMedicationProcurementSuggestionServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private EligibleStockSnapshotService eligibleStockSnapshotService;

    @Mock
    private OperationalReportQueryRepository operationalReportQueryRepository;

    @Mock
    private ClockPort clockPort;

    private GetMedicationProcurementSuggestionService service;
    private Instant now;
    private UUID medicineId1;
    private UUID medicineId2;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        medicineId1 = UUID.randomUUID();
        medicineId2 = UUID.randomUUID();

        service = new GetMedicationProcurementSuggestionService(
                medicineRepository,
                eligibleStockSnapshotService,
                operationalReportQueryRepository,
                new MedicationProcurementSuggestionCalculator(),
                clockPort
        );
    }

    @Test
    @DisplayName("Gợi ý số lượng cần mua thành công dựa trên tồn khả dụng, tồn tối thiểu và lịch sử cấp phát (TC-01)")
    void getSuggestionsSuccess() {
        Medicine med1 = Medicine.restore(
                medicineId1,
                "MED01",
                "Thuốc A",
                "Hoạt chất A",
                "100mg",
                com.benhsoan.domain.medicine.enums.DosageForm.TABLET,
                "Hộp",
                com.benhsoan.domain.medicine.enums.AdministrationRoute.ORAL,
                true,
                now,
                null,
                30,
                100,
                false
        );

        Medicine med2 = Medicine.restore(
                medicineId2,
                "MED02",
                "Thuốc B",
                "Hoạt chất B",
                "200mg",
                com.benhsoan.domain.medicine.enums.DosageForm.CAPSULE,
                "Vỉ",
                com.benhsoan.domain.medicine.enums.AdministrationRoute.ORAL,
                true,
                now,
                null,
                150,
                50,
                false
        );

        when(clockPort.now()).thenReturn(now);
        when(medicineRepository.findAllActive()).thenReturn(List.of(med1, med2));
        when(eligibleStockSnapshotService.snapshotEligibleStockQuantities(any(), any()))
                .thenReturn(Map.of(medicineId1, 30, medicineId2, 150));

        // Lịch sử cấp phát kỳ trước: Thuốc 1 cấp phát 80, Thuốc 2 cấp phát 10
        TopMedicineSummary summary1 = new TopMedicineSummary(medicineId1, "MED01", "Thuốc A", 80L);
        TopMedicineSummary summary2 = new TopMedicineSummary(medicineId2, "MED02", "Thuốc B", 10L);
        when(operationalReportQueryRepository.findTopDispensedMedicines(any(), any()))
                .thenReturn(List.of(summary1, summary2));

        // Khi onlyBelowThreshold = true:
        // Thuốc 1: eligible=30, min=100, consumption=80 -> target = 180 -> suggested = 150. Được đưa vào gợi ý.
        // Thuốc 2: eligible=150, min=50, consumption=10 -> target = 60 -> suggested = 0. Tồn 150 > min 50 -> bị lọc bỏ.
        ProcurementSuggestionResult result = service.getSuggestions(
                LocalDate.now().minusDays(30),
                LocalDate.now(),
                true
        );

        assertNotNull(result);
        assertEquals(1, result.totalItems());
        assertEquals("MED01", result.items().get(0).medicineCode());
        assertEquals(30, result.items().get(0).eligibleStock());
        assertEquals(100, result.items().get(0).minStockThreshold());
        assertEquals(80, result.items().get(0).previousPeriodConsumption());
        assertEquals(150, result.items().get(0).suggestedQuantity());
    }
}
