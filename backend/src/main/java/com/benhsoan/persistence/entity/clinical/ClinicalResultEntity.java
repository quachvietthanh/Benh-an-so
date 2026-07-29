package com.benhsoan.persistence.entity.clinical;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.benhsoan.domain.clinical.enums.ClinicalResultAbnormalFlag;
import com.benhsoan.domain.clinical.enums.ClinicalResultStatus;
import com.benhsoan.domain.clinical.enums.ClinicalResultType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clinical_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicalResultEntity {

    @Id
    @Column(columnDefinition = "BINARY(16)")
    UUID id;
    @Column(name = "clinical_order_item_id", nullable = false, columnDefinition = "BINARY(16)")
    UUID clinicalOrderItemId;
    @Column(name = "visit_id", nullable = false, columnDefinition = "BINARY(16)")
    UUID visitId;
    @Enumerated(EnumType.STRING)
    @Column(name = "result_type", nullable = false, length = 30)
    ClinicalResultType resultType;
    @Column(name = "numeric_value", precision = 18, scale = 4)
    BigDecimal numericValue;
    @Column(name = "text_value", columnDefinition = "TEXT")
    String textValue;
    @Column(length = 50)
    String unit;
    @Column(name = "reference_range", length = 255)
    String referenceRange;
    @Enumerated(EnumType.STRING)
    @Column(name = "abnormal_flag", nullable = false, length = 30)
    ClinicalResultAbnormalFlag abnormalFlag;
    @Column(columnDefinition = "TEXT")
    String conclusion;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    ClinicalResultStatus status;
    @Column(name = "entered_by", nullable = false, columnDefinition = "BINARY(16)")
    UUID enteredBy;
    @Column(name = "entered_at", nullable = false)
    Instant enteredAt;
    @Column(name = "updated_by", columnDefinition = "BINARY(16)")
    UUID updatedBy;
    @Column(name = "updated_at")
    Instant updatedAt;
}
