package com.benhsoan.application.ucservice.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MedicationProcurementSuggestionCalculatorTest {

    private MedicationProcurementSuggestionCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new MedicationProcurementSuggestionCalculator();
    }

    @Test
    @DisplayName("Tính số lượng cần mua khi tồn kho thiếu hụt so với nhu cầu và ngưỡng tối thiểu")
    void calculateDeficitSuggestedQuantity() {
        // Tồn khả dụng: 30, Tồn tối thiểu: 100, Đã cấp phát kỳ trước: 80
        // Nhu cầu dự trù = 80 + 100 = 180. Cần mua = 180 - 30 = 150.
        int suggested = calculator.calculateSuggestedQuantity(30, 100, 80);
        assertEquals(150, suggested);
    }

    @Test
    @DisplayName("Tính số lượng cần mua = 0 khi tồn khả dụng đã đủ đáp ứng")
    void calculateZeroWhenSufficientStock() {
        // Tồn khả dụng: 200, Tồn tối thiểu: 50, Đã cấp phát kỳ trước: 50
        // Nhu cầu = 100. Tồn 200 > 100 -> cần mua = 0.
        int suggested = calculator.calculateSuggestedQuantity(200, 50, 50);
        assertEquals(0, suggested);
    }

    @Test
    @DisplayName("Tính số lượng cần mua khi kỳ trước không phát sinh cấp phát nhưng dưới tồn tối thiểu")
    void calculateWhenNoPastConsumption() {
        // Tồn khả dụng: 20, Tồn tối thiểu: 50, Đã cấp phát kỳ trước: 0
        // Cần mua bù tồn tối thiểu = 50 - 20 = 30.
        int suggested = calculator.calculateSuggestedQuantity(20, 50, 0);
        assertEquals(30, suggested);
    }

    @Test
    @DisplayName("Lọc danh sách theo tùy chọn onlyBelowThreshold")
    void testShouldIncludeInSuggestion() {
        // Thuốc dưới ngưỡng tồn: eligible=40, min=100, suggested=100 -> bao gồm
        assertTrue(calculator.shouldIncludeInSuggestion(40, 100, 100, true));

        // Thuốc vượt ngưỡng nhưng có nhu cầu mua lớn do tiêu thụ cao: eligible=120, min=100, suggested=30 -> bao gồm
        assertTrue(calculator.shouldIncludeInSuggestion(120, 100, 30, true));

        // Thuốc tồn nhiều, không có nhu cầu: eligible=200, min=100, suggested=0 -> loại bỏ nếu onlyBelowThreshold=true
        assertFalse(calculator.shouldIncludeInSuggestion(200, 100, 0, true));

        // Nếu onlyBelowThreshold=false -> luôn bao gồm để Dược sĩ tự chọn
        assertTrue(calculator.shouldIncludeInSuggestion(200, 100, 0, false));
    }
}
