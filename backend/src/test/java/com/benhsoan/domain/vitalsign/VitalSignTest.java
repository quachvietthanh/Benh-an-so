package com.benhsoan.domain.vitalsign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.vitalsign.enums.VitalSignAbnormalFlag;

class VitalSignTest {

    private static final UUID VISIT_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();

    @Test
    @DisplayName("TC-01: Tạo thành công chỉ số sinh tồn với đầy đủ giá trị hợp lệ")
    void createsVitalSignSuccessfullyWhenDataIsValid() {
        VitalSign vs = VitalSign.create(
                VISIT_ID,
                PATIENT_ID,
                null,
                75,
                120,
                80,
                new BigDecimal("37.0"),
                16,
                new BigDecimal("65.0"),
                new BigDecimal("170.0"),
                98,
                "Khám định kỳ bình thường",
                DOCTOR_ID,
                Instant.now()
        );

        assertNotNull(vs.getId());
        assertEquals(VISIT_ID, vs.getVisitId());
        assertEquals(PATIENT_ID, vs.getPatientId());
        assertEquals(75, vs.getPulse());
        assertEquals(120, vs.getBloodPressureSystolic());
        assertEquals(80, vs.getBloodPressureDiastolic());
        assertEquals(new BigDecimal("37.0"), vs.getTemperature());
        assertEquals(16, vs.getRespiratoryRate());
        assertEquals(new BigDecimal("65.0"), vs.getWeight());
        assertEquals(new BigDecimal("170.0"), vs.getHeight());
        assertEquals(98, vs.getSpo2());
        // BMI = 65 / (1.7^2) = 65 / 2.89 = 22.5
        assertEquals(new BigDecimal("22.5"), vs.getBmi());
        assertFalse(vs.isAbnormal());
        assertTrue(vs.getAbnormalFlags().isEmpty());
    }

    @Test
    @DisplayName("TC-02: Từ chối nhiệt độ 90 độ C (ngoài khoảng hợp lệ 30.0 - 45.0)")
    void rejectsTemperatureOutOfValidRange() {
        ValidationException ex = assertThrows(ValidationException.class, () -> VitalSign.create(
                VISIT_ID,
                PATIENT_ID,
                null,
                75,
                120,
                80,
                new BigDecimal("90.0"), // 90°C invalid
                16,
                new BigDecimal("60.0"),
                new BigDecimal("165.0"),
                98,
                null,
                DOCTOR_ID,
                Instant.now()
        ));

        assertTrue(ex.getMessage().contains("Nhiệt độ ngoài khoảng hợp lệ"));
    }

    @Test
    @DisplayName("TC-02b: Từ chối khi huyết áp tâm thu nhỏ hơn hoặc bằng tâm trương")
    void rejectsWhenSystolicLessOrEqualToDiastolic() {
        ValidationException ex = assertThrows(ValidationException.class, () -> VitalSign.create(
                VISIT_ID,
                PATIENT_ID,
                null,
                75,
                80,
                120, // systolic 80 < diastolic 120
                new BigDecimal("37.0"),
                16,
                new BigDecimal("60.0"),
                new BigDecimal("165.0"),
                98,
                null,
                DOCTOR_ID,
                Instant.now()
        ));

        assertTrue(ex.getMessage().contains("Huyết áp tâm thu phải lớn hơn"));
    }

    @Test
    @DisplayName("TC-02c: Từ chối khi tất cả các chỉ số đều null")
    void rejectsWhenAllMeasurementsAreNull() {
        ValidationException ex = assertThrows(ValidationException.class, () -> VitalSign.create(
                VISIT_ID,
                PATIENT_ID,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                DOCTOR_ID,
                Instant.now()
        ));

        assertTrue(ex.getMessage().contains("Cần nhập ít nhất một chỉ số sinh tồn"));
    }

    @Test
    @DisplayName("TC-03: Đánh dấu bất thường khi huyết áp vượt ngưỡng tham chiếu (150/95 mmHg)")
    void marksAbnormalWhenBloodPressureExceedsReferenceThreshold() {
        VitalSign vs = VitalSign.create(
                VISIT_ID,
                PATIENT_ID,
                null,
                75,
                150, // systolic >= 140 -> HYPERTENSION
                95,  // diastolic >= 90 -> HYPERTENSION
                new BigDecimal("36.8"),
                16,
                new BigDecimal("70.0"),
                new BigDecimal("170.0"),
                98,
                "Huyết áp cao",
                DOCTOR_ID,
                Instant.now()
        );

        assertTrue(vs.isAbnormal());
        assertTrue(vs.getAbnormalFlags().contains(VitalSignAbnormalFlag.HYPERTENSION));
    }

    @Test
    @DisplayName("TC-03b: Đánh dấu sốt (FEVER) và nhịp tim nhanh (TACHYCARDIA)")
    void marksFeverAndTachycardia() {
        VitalSign vs = VitalSign.create(
                VISIT_ID,
                PATIENT_ID,
                null,
                115, // > 100 bpm -> TACHYCARDIA
                120,
                80,
                new BigDecimal("38.8"), // > 37.5°C -> FEVER
                24, // > 20 -> TACHYPNEA
                new BigDecimal("60.0"),
                new BigDecimal("165.0"),
                92, // < 95% -> HYPOXEMIA
                "Sốt cao, thở gấp",
                DOCTOR_ID,
                Instant.now()
        );

        assertTrue(vs.isAbnormal());
        assertTrue(vs.getAbnormalFlags().contains(VitalSignAbnormalFlag.FEVER));
        assertTrue(vs.getAbnormalFlags().contains(VitalSignAbnormalFlag.TACHYCARDIA));
        assertTrue(vs.getAbnormalFlags().contains(VitalSignAbnormalFlag.TACHYPNEA));
        assertTrue(vs.getAbnormalFlags().contains(VitalSignAbnormalFlag.HYPOXEMIA));
    }

    @Test
    @DisplayName("Cập nhật chỉ số sinh tồn và tính toán lại cờ cảnh báo")
    void updatesMeasurementsAndRecalculatesFlags() {
        VitalSign vs = VitalSign.create(
                VISIT_ID,
                PATIENT_ID,
                null,
                75,
                150,
                95,
                new BigDecimal("37.0"),
                16,
                new BigDecimal("65.0"),
                new BigDecimal("170.0"),
                98,
                null,
                DOCTOR_ID,
                Instant.now()
        );
        assertTrue(vs.isAbnormal());

        // Bác sĩ cập nhật lại sau khi nghỉ ngơi
        UUID doctor2 = UUID.randomUUID();
        Instant now = Instant.now();
        vs.update(
                72,
                118,
                78,
                new BigDecimal("36.8"),
                16,
                new BigDecimal("65.0"),
                new BigDecimal("170.0"),
                98,
                "Huyết áp đã ổn định lại",
                doctor2,
                now
        );

        assertFalse(vs.isAbnormal());
        assertTrue(vs.getAbnormalFlags().isEmpty());
        assertEquals(doctor2, vs.getUpdatedBy());
        assertEquals(now, vs.getUpdatedAt());
    }
}
