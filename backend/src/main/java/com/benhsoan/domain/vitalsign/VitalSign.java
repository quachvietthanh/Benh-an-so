package com.benhsoan.domain.vitalsign;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.benhsoan.domain.shared.exception.ValidationException;
import com.benhsoan.domain.vitalsign.enums.VitalSignAbnormalFlag;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(of = "id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VitalSign {

    private UUID id;
    private UUID visitId;
    private UUID patientId;
    private UUID medicalRecordId;

    private Integer pulse;
    private Integer bloodPressureSystolic;
    private Integer bloodPressureDiastolic;
    private BigDecimal temperature;
    private Integer respiratoryRate;
    private BigDecimal weight;
    private BigDecimal height;
    private BigDecimal bmi;
    private Integer spo2;

    private boolean abnormal;
    private List<VitalSignAbnormalFlag> abnormalFlags;
    private String note;

    private UUID recordedBy;
    private Instant recordedAt;
    private UUID updatedBy;
    private Instant updatedAt;

    private VitalSign(
            UUID id,
            UUID visitId,
            UUID patientId,
            UUID medicalRecordId,
            Integer pulse,
            Integer bloodPressureSystolic,
            Integer bloodPressureDiastolic,
            BigDecimal temperature,
            Integer respiratoryRate,
            BigDecimal weight,
            BigDecimal height,
            BigDecimal bmi,
            Integer spo2,
            boolean abnormal,
            List<VitalSignAbnormalFlag> abnormalFlags,
            String note,
            UUID recordedBy,
            Instant recordedAt,
            UUID updatedBy,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "ID must not be null");
        this.visitId = Objects.requireNonNull(visitId, "Visit ID must not be null");
        this.patientId = Objects.requireNonNull(patientId, "Patient ID must not be null");
        this.medicalRecordId = medicalRecordId;
        this.pulse = pulse;
        this.bloodPressureSystolic = bloodPressureSystolic;
        this.bloodPressureDiastolic = bloodPressureDiastolic;
        this.temperature = temperature;
        this.respiratoryRate = respiratoryRate;
        this.weight = weight;
        this.height = height;
        this.bmi = bmi;
        this.spo2 = spo2;
        this.abnormal = abnormal;
        this.abnormalFlags = abnormalFlags != null ? new ArrayList<>(abnormalFlags) : new ArrayList<>();
        this.note = note;
        this.recordedBy = Objects.requireNonNull(recordedBy, "RecordedBy must not be null");
        this.recordedAt = Objects.requireNonNull(recordedAt, "RecordedAt must not be null");
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public static VitalSign create(
            UUID visitId,
            UUID patientId,
            UUID medicalRecordId,
            Integer pulse,
            Integer bloodPressureSystolic,
            Integer bloodPressureDiastolic,
            BigDecimal temperature,
            Integer respiratoryRate,
            BigDecimal weight,
            BigDecimal height,
            Integer spo2,
            String note,
            UUID recordedBy,
            Instant recordedAt
    ) {
        validateMeasurements(pulse, bloodPressureSystolic, bloodPressureDiastolic, temperature,
                respiratoryRate, weight, height, spo2);

        BigDecimal calculatedBmi = calculateBmi(weight, height);
        List<VitalSignAbnormalFlag> flags = evaluateAbnormalFlags(pulse, bloodPressureSystolic,
                bloodPressureDiastolic, temperature, respiratoryRate, calculatedBmi, spo2);
        boolean isAbnormal = !flags.isEmpty();

        return new VitalSign(
                UUID.randomUUID(),
                visitId,
                patientId,
                medicalRecordId,
                pulse,
                bloodPressureSystolic,
                bloodPressureDiastolic,
                temperature,
                respiratoryRate,
                weight,
                height,
                calculatedBmi,
                spo2,
                isAbnormal,
                flags,
                note,
                recordedBy,
                recordedAt,
                null,
                null
        );
    }

    public static VitalSign restore(
            UUID id,
            UUID visitId,
            UUID patientId,
            UUID medicalRecordId,
            Integer pulse,
            Integer bloodPressureSystolic,
            Integer bloodPressureDiastolic,
            BigDecimal temperature,
            Integer respiratoryRate,
            BigDecimal weight,
            BigDecimal height,
            BigDecimal bmi,
            Integer spo2,
            boolean abnormal,
            List<VitalSignAbnormalFlag> abnormalFlags,
            String note,
            UUID recordedBy,
            Instant recordedAt,
            UUID updatedBy,
            Instant updatedAt
    ) {
        return new VitalSign(
                id,
                visitId,
                patientId,
                medicalRecordId,
                pulse,
                bloodPressureSystolic,
                bloodPressureDiastolic,
                temperature,
                respiratoryRate,
                weight,
                height,
                bmi,
                spo2,
                abnormal,
                abnormalFlags,
                note,
                recordedBy,
                recordedAt,
                updatedBy,
                updatedAt
        );
    }

    public void update(
            Integer pulse,
            Integer bloodPressureSystolic,
            Integer bloodPressureDiastolic,
            BigDecimal temperature,
            Integer respiratoryRate,
            BigDecimal weight,
            BigDecimal height,
            Integer spo2,
            String note,
            UUID updatedBy,
            Instant updatedAt
    ) {
        validateMeasurements(pulse, bloodPressureSystolic, bloodPressureDiastolic, temperature,
                respiratoryRate, weight, height, spo2);

        this.pulse = pulse;
        this.bloodPressureSystolic = bloodPressureSystolic;
        this.bloodPressureDiastolic = bloodPressureDiastolic;
        this.temperature = temperature;
        this.respiratoryRate = respiratoryRate;
        this.weight = weight;
        this.height = height;
        this.bmi = calculateBmi(weight, height);
        this.spo2 = spo2;
        this.note = note;

        this.abnormalFlags = evaluateAbnormalFlags(pulse, bloodPressureSystolic, bloodPressureDiastolic,
                temperature, respiratoryRate, this.bmi, spo2);
        this.abnormal = !this.abnormalFlags.isEmpty();

        this.updatedBy = Objects.requireNonNull(updatedBy, "UpdatedBy must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "UpdatedAt must not be null");
    }

    public void attachMedicalRecord(UUID medicalRecordId) {
        this.medicalRecordId = medicalRecordId;
    }

    public List<VitalSignAbnormalFlag> getAbnormalFlags() {
        return Collections.unmodifiableList(abnormalFlags);
    }

    public static void validateMeasurements(
            Integer pulse,
            Integer bloodPressureSystolic,
            Integer bloodPressureDiastolic,
            BigDecimal temperature,
            Integer respiratoryRate,
            BigDecimal weight,
            BigDecimal height,
            Integer spo2
    ) {
        boolean allNull = pulse == null && bloodPressureSystolic == null && bloodPressureDiastolic == null
                && temperature == null && respiratoryRate == null && weight == null && height == null && spo2 == null;
        if (allNull) {
            throw new ValidationException("vitalSigns", "Cần nhập ít nhất một chỉ số sinh tồn.");
        }

        if (pulse != null && (pulse < 30 || pulse > 250)) {
            throw new ValidationException("pulse", "Mạch ngoài khoảng hợp lệ (30 - 250 lần/phút).");
        }

        if (bloodPressureSystolic != null && (bloodPressureSystolic < 50 || bloodPressureSystolic > 260)) {
            throw new ValidationException("bloodPressureSystolic", "Huyết áp tâm thu ngoài khoảng hợp lệ (50 - 260 mmHg).");
        }

        if (bloodPressureDiastolic != null && (bloodPressureDiastolic < 30 || bloodPressureDiastolic > 150)) {
            throw new ValidationException("bloodPressureDiastolic", "Huyết áp tâm trương ngoài khoảng hợp lệ (30 - 150 mmHg).");
        }

        if (bloodPressureSystolic != null && bloodPressureDiastolic != null) {
            if (bloodPressureSystolic <= bloodPressureDiastolic) {
                throw new ValidationException("bloodPressure", "Huyết áp tâm thu phải lớn hơn huyết áp tâm trương.");
            }
        }

        if (temperature != null && (temperature.compareTo(BigDecimal.valueOf(30.0)) < 0
                || temperature.compareTo(BigDecimal.valueOf(45.0)) > 0)) {
            throw new ValidationException("temperature", "Nhiệt độ ngoài khoảng hợp lệ (30.0 - 45.0 °C).");
        }

        if (respiratoryRate != null && (respiratoryRate < 5 || respiratoryRate > 60)) {
            throw new ValidationException("respiratoryRate", "Nhịp thở ngoài khoảng hợp lệ (5 - 60 lần/phút).");
        }

        if (weight != null && (weight.compareTo(BigDecimal.valueOf(0.5)) < 0
                || weight.compareTo(BigDecimal.valueOf(300.0)) > 0)) {
            throw new ValidationException("weight", "Cân nặng ngoài khoảng hợp lệ (0.5 - 300.0 kg).");
        }

        if (height != null && (height.compareTo(BigDecimal.valueOf(20.0)) < 0
                || height.compareTo(BigDecimal.valueOf(250.0)) > 0)) {
            throw new ValidationException("height", "Chiều cao ngoài khoảng hợp lệ (20.0 - 250.0 cm).");
        }

        if (spo2 != null && (spo2 < 50 || spo2 > 100)) {
            throw new ValidationException("spo2", "SpO2 ngoài khoảng hợp lệ (50 - 100 %).");
        }
    }

    public static BigDecimal calculateBmi(BigDecimal weight, BigDecimal height) {
        if (weight == null || height == null || height.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        // height in meters = height (cm) / 100
        BigDecimal heightInMeters = height.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal heightSquared = heightInMeters.multiply(heightInMeters);
        if (heightSquared.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return weight.divide(heightSquared, 1, RoundingMode.HALF_UP);
    }

    public static List<VitalSignAbnormalFlag> evaluateAbnormalFlags(
            Integer pulse,
            Integer bloodPressureSystolic,
            Integer bloodPressureDiastolic,
            BigDecimal temperature,
            Integer respiratoryRate,
            BigDecimal bmi,
            Integer spo2
    ) {
        List<VitalSignAbnormalFlag> flags = new ArrayList<>();

        // Blood pressure evaluation
        if ((bloodPressureSystolic != null && bloodPressureSystolic >= 140)
                || (bloodPressureDiastolic != null && bloodPressureDiastolic >= 90)) {
            flags.add(VitalSignAbnormalFlag.HYPERTENSION);
        } else if ((bloodPressureSystolic != null && bloodPressureSystolic < 90)
                || (bloodPressureDiastolic != null && bloodPressureDiastolic < 60)) {
            flags.add(VitalSignAbnormalFlag.HYPOTENSION);
        }

        // Pulse evaluation
        if (pulse != null) {
            if (pulse > 100) {
                flags.add(VitalSignAbnormalFlag.TACHYCARDIA);
            } else if (pulse < 60) {
                flags.add(VitalSignAbnormalFlag.BRADYCARDIA);
            }
        }

        // Temperature evaluation
        if (temperature != null) {
            if (temperature.compareTo(BigDecimal.valueOf(37.5)) > 0) {
                flags.add(VitalSignAbnormalFlag.FEVER);
            } else if (temperature.compareTo(BigDecimal.valueOf(36.0)) < 0) {
                flags.add(VitalSignAbnormalFlag.HYPOTHERMIA);
            }
        }

        // Respiratory rate evaluation
        if (respiratoryRate != null) {
            if (respiratoryRate > 20) {
                flags.add(VitalSignAbnormalFlag.TACHYPNEA);
            } else if (respiratoryRate < 12) {
                flags.add(VitalSignAbnormalFlag.BRADYPNEA);
            }
        }

        // SpO2 evaluation
        if (spo2 != null && spo2 < 95) {
            flags.add(VitalSignAbnormalFlag.HYPOXEMIA);
        }

        // BMI evaluation
        if (bmi != null) {
            if (bmi.compareTo(BigDecimal.valueOf(18.5)) < 0) {
                flags.add(VitalSignAbnormalFlag.UNDERWEIGHT);
            } else if (bmi.compareTo(BigDecimal.valueOf(25.0)) >= 0) {
                flags.add(VitalSignAbnormalFlag.OVERWEIGHT);
            }
        }

        return flags;
    }
}
