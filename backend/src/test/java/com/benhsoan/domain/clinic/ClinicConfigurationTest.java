package com.benhsoan.domain.clinic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.shared.exception.ValidationException;

class ClinicConfigurationTest {

    private static final Instant NOW = Instant.parse("2026-09-16T08:00:00Z");

    @Test
    @DisplayName("Khởi tạo cấu hình với signingDeadlineHours mặc định là 24")
    void defaultSigningDeadlineHoursIs24() {
        ClinicConfiguration config = ClinicConfiguration.create(
                "Phong kham Benh So An",
                "Thai Nguyen",
                "0345678910",
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                NOW
        );

        assertEquals(24, config.getSigningDeadlineHours());
    }

    @Test
    @DisplayName("Khởi tạo cấu hình với signingDeadlineHours tùy chỉnh hợp lệ")
    void customSigningDeadlineHoursValid() {
        ClinicConfiguration config = ClinicConfiguration.create(
                "Phong kham Benh So An",
                "Thai Nguyen",
                "0345678910",
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                10,
                48,
                NOW
        );

        assertEquals(48, config.getSigningDeadlineHours());
    }

    @Test
    @DisplayName("Từ chối signingDeadlineHours nhỏ hơn 1")
    void rejectsSigningDeadlineHoursLessThanOne() {
        assertThrows(ValidationException.class, () -> ClinicConfiguration.create(
                "Phong kham Benh So An",
                "Thai Nguyen",
                "0345678910",
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                10,
                0,
                NOW
        ));
    }

    @Test
    @DisplayName("Cập nhật signingDeadlineHours thành công")
    void updateSigningDeadlineHoursSuccessfully() {
        ClinicConfiguration config = ClinicConfiguration.create(
                "Phong kham Benh So An",
                "Thai Nguyen",
                "0345678910",
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                NOW
        );

        config.updateSigningDeadlineHours(12, NOW.plusSeconds(3600));
        assertEquals(12, config.getSigningDeadlineHours());
    }
}
