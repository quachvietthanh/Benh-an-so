package com.benhsoan.persistence.entity.medicine;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "medicine_max_daily_dose_missing_flags",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_medicine_mdd_missing_flag",
                columnNames = {"medicine_id", "missing_reason"}
        ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineMaxDailyDoseMissingDataEntity {

    @Id
    @Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "medicine_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID medicineId;

    @Column(name = "active_ingredient", nullable = false, length = 255)
    private String activeIngredient;

    @Column(name = "missing_reason", nullable = false, length = 50)
    private String missingReason;

    @Column(name = "first_detected_at", nullable = false)
    private Instant firstDetectedAt;

    @Column(name = "last_detected_at", nullable = false)
    private Instant lastDetectedAt;
}
