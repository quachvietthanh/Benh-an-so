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

    @Test
    @DisplayName("Khởi tạo cấu hình với sessionIdleTimeoutMinutes mặc định là 30")
    void defaultSessionIdleTimeoutMinutesIs30() {
        ClinicConfiguration config = ClinicConfiguration.create(
                "Phong kham Benh So An",
                "Thai Nguyen",
                "0345678910",
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                NOW
        );

        assertEquals(30, config.getSessionIdleTimeoutMinutes());
    }

    @Test
    @DisplayName("Khởi tạo cấu hình với sessionIdleTimeoutMinutes tùy chỉnh hợp lệ")
    void customSessionIdleTimeoutMinutesValid() {
        ClinicConfiguration config = ClinicConfiguration.create(
                "Phong kham Benh So An",
                "Thai Nguyen",
                "0345678910",
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                10,
                24,
                60,
                NOW
        );

        assertEquals(60, config.getSessionIdleTimeoutMinutes());
    }

    @Test
    @DisplayName("Từ chối sessionIdleTimeoutMinutes nhỏ hơn 5 phút")
    void rejectsSessionIdleTimeoutMinutesLessThan5() {
        assertThrows(ValidationException.class, () -> ClinicConfiguration.create(
                "Phong kham Benh So An",
                "Thai Nguyen",
                "0345678910",
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                10,
                24,
                4,
                NOW
        ));
    }

    @Test
    @DisplayName("Từ chối sessionIdleTimeoutMinutes lớn hơn 1440 phút")
    void rejectsSessionIdleTimeoutMinutesGreaterThan1440() {
        assertThrows(ValidationException.class, () -> ClinicConfiguration.create(
                "Phong kham Benh So An",
                "Thai Nguyen",
                "0345678910",
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                10,
                24,
                1441,
                NOW
        ));
    }

    @Test
    @DisplayName("Cập nhật sessionIdleTimeoutMinutes thành công")
    void updateSessionIdleTimeoutMinutesSuccessfully() {
        ClinicConfiguration config = ClinicConfiguration.create(
                "Phong kham Benh So An",
                "Thai Nguyen",
                "0345678910",
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                NOW
        );

        config.updateSessionIdleTimeoutMinutes(45, NOW.plusSeconds(3600));
        assertEquals(45, config.getSessionIdleTimeoutMinutes());
    }
}
